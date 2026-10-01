import assert from 'node:assert/strict'

// Opt-in integration check: adds one fictional plan/order and two results.
const base = process.env.PES_API_URL ?? 'http://localhost:8080'
function client() {
  const jar = new Map()
  return async (path, method = 'GET', body) => {
    const headers = { Cookie: [...jar].map(([k, v]) => `${k}=${v}`).join('; ') }
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = decodeURIComponent(jar.get('XSRF-TOKEN') ?? '')
    if (body) headers['Content-Type'] = typeof body === 'string' ? 'application/x-www-form-urlencoded' : 'application/json'
    const response = await fetch(base + path, {
      method, headers, body: body ? (typeof body === 'string' ? body : JSON.stringify(body)) : undefined,
    })
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
  assert.equal((await request('/api/auth/login', 'POST', new URLSearchParams({
    username, password: process.env.PES_DEMO_PASSWORD ?? 'pes-demo-1234',
  }).toString())).status, 204)
  await request('/api/auth/csrf')
  return request
}
const admin = await login('admin')
const worker = await login('worker')
const before = (await admin('/api/dashboard/summary')).data
const products = (await admin('/api/products')).data.content
const processes = (await admin('/api/processes')).data.content
const workers = (await admin('/api/workers')).data
assert.ok(products?.length && processes?.length && workers?.length, 'Register master data first')
const dueDate = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10)
const plan = await admin('/api/production-plans', 'POST', { productId: products[0].id, dueDate, targetQuantity: 10 })
assert.equal(plan.status, 201, JSON.stringify(plan))
assert.equal((await admin(`/api/production-plans/${plan.data.id}/confirm`, 'POST')).status, 200)
const order = await admin('/api/work-orders', 'POST', {
  productionPlanId: plan.data.id, productionProcessId: processes[0].id,
  assignedWorkerId: workers.find(user => user.username === 'worker').id, targetQuantity: 10,
})
assert.equal(order.status, 201, JSON.stringify(order))
const path = `/api/work-orders/${order.data.id}`
assert.equal((await worker(path + '/start', 'POST')).status, 200)
assert.equal((await worker('/api/admin/users')).status, 403)
assert.equal((await worker(path + '/results', 'POST', { producedQuantity: 2, goodQuantity: 1, defectQuantity: 0 })).status, 400)
assert.equal((await worker(path + '/results', 'POST', { producedQuantity: 1, goodQuantity: 2, defectQuantity: -1 })).status, 400)
assert.equal((await worker(path + '/complete', 'POST')).status, 409)
const results = await Promise.all([1, 2].map(() => worker(path + '/results', 'POST', {
  producedQuantity: 6, goodQuantity: 5, defectQuantity: 1,
})))
assert.deepEqual(results.map(result => result.status).sort(), [201, 409], JSON.stringify(results))
assert.equal((await worker(path + '/results', 'POST', { producedQuantity: 4, goodQuantity: 4, defectQuantity: 0 })).status, 201)
const completion = await Promise.all([1, 2].map(() => worker(path + '/complete', 'POST')))
assert.deepEqual(completion.map(result => result.status).sort(), [200, 409], JSON.stringify(completion))
assert.equal((await worker(path + '/start', 'POST')).status, 409)
assert.equal((await worker(path + '/results', 'POST', { producedQuantity: 1, goodQuantity: 1, defectQuantity: 0 })).status, 409)
const after = (await admin('/api/dashboard/summary')).data
for (const [key, delta] of Object.entries({ planned: 10, ordered: 10, produced: 10, good: 9, defect: 1 })) {
  assert.equal(after.quantities[key] - before.quantities[key], delta, key)
}
const orders = (await admin('/api/work-orders?search=' + order.data.workOrderNumber)).data.content
assert.equal(orders[0].producedQuantity, 10)
assert.equal(orders[0].status, 'COMPLETED')
const recorded = (await admin('/api/production-results?search=' + order.data.workOrderNumber)).data.content
assert.equal(recorded.reduce((total, result) => total + result.producedQuantity, 0), 10)
console.log(JSON.stringify({ passed: true, plan: plan.data.planNumber, order: order.data.workOrderNumber,
  results: recorded.length, concurrentResults: results.map(result => result.status),
  concurrentCompletion: completion.map(result => result.status), produced: 10, good: 9, defect: 1 }))
