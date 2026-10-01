import assert from 'node:assert/strict'

// Development-only integration check. Keeps fictional records; run without other writers.
const base = process.env.PES_API_URL ?? 'http://localhost:8080'
function client() {
  const jar = new Map()
  return async (path, method = 'GET', body) => {
    const headers = { Cookie: [...jar].map(([key, value]) => `${key}=${value}`).join('; ') }
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = decodeURIComponent(jar.get('XSRF-TOKEN') ?? '')
    if (body) headers['Content-Type'] = typeof body === 'string' ? 'application/x-www-form-urlencoded' : 'application/json'
    const response = await fetch(base + path, { method, headers,
      body: body ? (typeof body === 'string' ? body : JSON.stringify(body)) : undefined,
      signal: AbortSignal.timeout(15000) })
    for (const cookie of response.headers.getSetCookie()) {
      const pair = cookie.split(';')[0]
      const index = pair.indexOf('=')
      jar.set(pair.slice(0, index), pair.slice(index + 1))
    }
    const text = await response.text()
    return { status: response.status, data: text ? JSON.parse(text) : null }
  }
}
async function login(username) {
  const request = client()
  assert.equal((await request('/api/auth/csrf')).status, 200)
  assert.equal((await request('/api/auth/login', 'POST', new URLSearchParams({ username,
    password: process.env.PES_DEMO_PASSWORD ?? 'pes-demo-1234' }).toString())).status, 204)
  await request('/api/auth/csrf')
  return request
}
const admin = await login('admin')
const worker = await login('worker')
assert.equal((await admin('/api/product-lots')).status, 200, 'Rebuild Phase 5A before running this script')
const before = (await admin('/api/dashboard/summary')).data
const products = (await admin('/api/products?size=100')).data.content
const processes = (await admin('/api/processes?size=100')).data.content
const workers = (await admin('/api/workers')).data
const product = products.find(item => item.active && item.unit === 'EACH')
const productionProcess = processes.find(item => item.active)
const assigned = workers.find(item => item.username === 'worker')
assert.ok(product && productionProcess && assigned, 'Register an active EACH product/process and demo worker first')
const plan = await admin('/api/production-plans', 'POST', { productId: product.id,
  dueDate: new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10), targetQuantity: 20 })
assert.equal(plan.status, 201, JSON.stringify(plan))
assert.equal((await admin(`/api/production-plans/${plan.data.id}/confirm`, 'POST')).status, 200)
const orders = []
for (let index = 0; index < 2; index++) {
  const response = await admin('/api/work-orders', 'POST', { productionPlanId: plan.data.id,
    productionProcessId: productionProcess.id, assignedWorkerId: assigned.id, targetQuantity: 10, lotTrackingEnabled: true })
  assert.equal(response.status, 201, JSON.stringify(response))
  assert.equal(response.data.lotTrackingEnabled, true)
  orders.push(response.data)
}
const lots = []
for (const quantity of [6, 14]) {
  const response = await admin('/api/material-lots', 'POST', {
    materialCode: 'RAW-DEMO', materialName: '가상 브래킷 소재', receivedQuantity: quantity })
  assert.equal(response.status, 201, JSON.stringify(response))
  lots.push(response.data)
}
const input = (index, lot, quantity) => worker(`/api/work-orders/${orders[index].id}/materials`, 'POST', {
  materialLotId: lot.id, inputQuantity: quantity })
assert.equal((await worker(`/api/work-orders/${orders[0].id}/start`, 'POST')).status, 409)
assert.equal((await input(0, lots[0], 0)).status, 400)
assert.equal((await worker('/api/material-lots', 'POST', { materialCode: 'RAW-XX', materialName: '금지', receivedQuantity: 1 })).status, 403)
const concurrent = await Promise.all([input(0, lots[0], 4), input(1, lots[0], 4)])
assert.deepEqual(concurrent.map(response => response.status).sort(), [201, 409], JSON.stringify(concurrent))
const winner = concurrent.findIndex(response => response.status === 201)
const loser = 1 - winner
assert.equal((await input(winner, lots[0], 1)).status, 409)
assert.equal((await input(loser, lots[0], 2)).status, 201)
assert.equal((await input(winner, lots[1], 7)).status, 409)
assert.equal((await input(winner, lots[1], 6)).status, 201)
assert.equal((await input(loser, lots[1], 8)).status, 201)
for (const order of orders) {
  const path = `/api/work-orders/${order.id}`
  assert.equal((await worker(path + '/start', 'POST')).status, 200)
  assert.equal((await worker(path + '/materials', 'POST', { materialLotId: lots[0].id, inputQuantity: 1 })).status, 409)
  for (const quantities of [{ producedQuantity: 6, goodQuantity: 5, defectQuantity: 1 },
    { producedQuantity: 4, goodQuantity: 4, defectQuantity: 0 }]) {
    assert.equal((await worker(path + '/results', 'POST', quantities)).status, 201)
  }
  assert.equal((await worker(path + '/complete', 'POST')).status, 200)
  const generated = await admin('/api/product-lots?search=' + order.workOrderNumber)
  assert.equal(generated.status, 200, JSON.stringify(generated))
  assert.equal(generated.data.totalElements, 2)
  for (const lot of generated.data.content) {
    const trace = await admin(`/api/product-lots/${lot.id}/trace`)
    assert.equal(trace.status, 200, JSON.stringify(trace))
    assert.equal(trace.data.materials.length, 2)
    assert.equal(trace.data.materials.reduce((sum, item) => sum + item.inputQuantity, 0), 10)
  }
}
for (const lot of lots) {
  const trace = await admin(`/api/material-lots/${lot.id}/trace`)
  assert.equal(trace.status, 200, JSON.stringify(trace))
  assert.equal(trace.data.inputs.length, 2)
  assert.equal(trace.data.productLots.length, 4)
  assert.equal(trace.data.inputs.reduce((sum, item) => sum + item.inputQuantity, 0), lot.receivedQuantity)
}
const after = (await admin('/api/dashboard/summary')).data
for (const [key, delta] of Object.entries({ planned: 20, ordered: 20, produced: 20, good: 18, defect: 2 })) {
  assert.equal(after.quantities[key] - before.quantities[key], delta, key)
}
console.log(JSON.stringify({ passed: true, plan: plan.data.planNumber, orders: orders.map(order => order.workOrderNumber),
  materialLots: lots.map(lot => lot.lotNumber), productLots: 4, concurrentInputs: concurrent.map(response => response.status),
  produced: 20, good: 18, defect: 2 }))
