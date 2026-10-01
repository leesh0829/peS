import assert from 'node:assert/strict'

// Development only: consumes two uninspected fictional LOTs from Phase 5A.
const base = process.env.PES_API_URL ?? 'http://localhost:8080'
async function login(username) {
  const cookies = new Map()
  const request = async (path, method = 'GET', body) => {
    const headers = { Cookie: [...cookies].map(([k, v]) => `${k}=${v}`).join('; ') }
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = decodeURIComponent(cookies.get('XSRF-TOKEN') ?? '')
    if (body) headers['Content-Type'] = typeof body === 'string' ? 'application/x-www-form-urlencoded' : 'application/json'
    const response = await fetch(base + path, { method, headers,
      body: body ? (typeof body === 'string' ? body : JSON.stringify(body)) : undefined,
      signal: AbortSignal.timeout(15000) })
    for (const cookie of response.headers.getSetCookie()) {
      const pair = cookie.split(';')[0], index = pair.indexOf('=')
      cookies.set(pair.slice(0, index), pair.slice(index + 1))
    }
    const text = await response.text()
    return { status: response.status, data: text ? JSON.parse(text) : null }
  }
  assert.equal((await request('/api/auth/csrf')).status, 200)
  assert.equal((await request('/api/auth/login', 'POST', new URLSearchParams({ username,
    password: process.env.PES_DEMO_PASSWORD ?? 'pes-demo-1234' }).toString())).status, 204)
  await request('/api/auth/csrf')
  return request
}
const admin = await login('admin'), worker = await login('worker')
const candidates = await admin('/api/inspection-candidates?size=100')
assert.equal(candidates.status, 200, JSON.stringify(candidates))
const failLot = candidates.data.content.find(lot => lot.result.producedQuantity === 6 && lot.result.goodQuantity === 5)
const passLot = candidates.data.content.find(lot => lot.result.producedQuantity === 4 && lot.result.goodQuantity === 4)
assert.ok(failLot && passLot, 'Run verify-phase5a.mjs to generate fresh 6/5/1 and 4/4/0 LOTs first')
const before = (await admin('/api/dashboard/summary')).data
const codes = []
for (const suffix of ['A', 'B']) {
  const code = await admin('/api/defect-codes', 'POST', { code: `DEMO-${Date.now()}-${suffix}`, name: `가상 검사 불량 ${suffix}` })
  assert.equal(code.status, 201, JSON.stringify(code))
  codes.push(code.data)
}
const path = `/api/product-lots/${failLot.id}/inspect`
const valid = { inspectedQuantity: 6, acceptedQuantity: 4, defects: codes.map(code => ({ defectCodeId: code.id, quantity: 1 })), note: '가상 전수검사 시연' }
for (const invalid of [
  { ...valid, acceptedQuantity: 3 },
  { ...valid, acceptedQuantity: -1 },
  { ...valid, inspectedQuantity: 5, acceptedQuantity: 3 },
  { inspectedQuantity: 6, acceptedQuantity: 6, defects: [] },
  { ...valid, defects: [{ defectCodeId: codes[0].id, quantity: 1 }, { defectCodeId: codes[0].id, quantity: 1 }] },
]) assert.equal((await admin(path, 'POST', invalid)).status, 400)
assert.equal((await worker(path, 'POST', valid)).status, 403)
assert.equal((await worker('/api/inspection-candidates')).status, 403)
assert.equal((await worker('/api/defect-codes', 'POST', { code: 'FORBIDDEN', name: '가상 금지' })).status, 403)
assert.deepEqual((await admin(`/api/product-lots/${failLot.id}/inspection`)).data, { inspection: null })
const concurrent = await Promise.all([admin(path, 'POST', valid), admin(path, 'POST', valid)])
assert.deepEqual(concurrent.map(r => r.status).sort(), [201, 409], JSON.stringify(concurrent))
const failure = concurrent.find(r => r.status === 201).data
assert.equal(failure.judgement, 'FAIL')
assert.equal(failure.rejectedQuantity, 2)
assert.equal(failure.defects.length, 2)
const pass = await admin(`/api/product-lots/${passLot.id}/inspect`, 'POST', { inspectedQuantity: 4, acceptedQuantity: 4, defects: [] })
assert.equal(pass.status, 201, JSON.stringify(pass))
assert.equal(pass.data.judgement, 'PASS')
for (const lot of [failLot, passLot]) {
  const detail = await worker(`/api/product-lots/${lot.id}/inspection`)
  assert.equal(detail.status, 200, JSON.stringify(detail))
  assert.equal(detail.data.inspection.productLot.id, lot.id)
  const trace = await admin(`/api/product-lots/${lot.id}/trace`)
  assert.equal(trace.status, 200)
}
for (const judgement of ['PASS', 'FAIL']) {
  const list = await admin(`/api/inspections?judgement=${judgement}`)
  assert.equal(list.status, 200, JSON.stringify(list))
  assert.ok(list.data.content.length > 0)
  assert.ok(list.data.content.every(item => item.judgement === judgement))
}
const remaining = (await admin('/api/inspection-candidates?size=100')).data.content
assert.ok(!remaining.some(lot => [failLot.id, passLot.id].includes(lot.id)))
assert.deepEqual((await admin('/api/dashboard/summary')).data, before, 'Inspection must not change production aggregates')
console.log('PASS: quality quantities, roles, concurrent 201/409, PASS/FAIL, LOT details, candidates and unchanged dashboard')
