import request from '@/utils/request'

export function compareMigrationResources(data) {
  return request({
    url: '/api/v1/migration/resources/compare',
    method: 'post',
    data
  })
}

export function createMigrationPlan(data) {
  return request({
    url: '/api/v1/migration/job/plan',
    method: 'post',
    data
  })
}

export function getMigrationProgress(jobId) {
  return request({
    url: '/api/v1/migration/job/' + jobId + '/progress',
    method: 'get'
  })
}

export function sendMigrationPasscode(data) {
  return request({
    url: '/api/v1/migration/job/send-passcode',
    method: 'post',
    data
  })
}

export function verifyMigrationPasscode(data) {
  return request({
    url: '/api/v1/migration/job/verify-passcode',
    method: 'post',
    data
  })
}

export function publishMigrationJob(data) {
  return request({
    url: '/api/v1/migration/job/apply',
    method: 'post',
    data
  })
}

export function listMigrationAuditLog(jobId) {
  return request({
    url: '/api/v1/migration/job/' + jobId + '/audit-log',
    method: 'get'
  })
}

export function listMigrationEnvironmentProfiles() {
  return request({
    url: '/api/v1/migration/env/profiles',
    method: 'get'
  })
}

export function getMigrationEnvironmentProfile(envKey) {
  return request({
    url: '/api/v1/migration/env/profiles/' + envKey,
    method: 'get'
  })
}

export function saveMigrationEnvironmentProfile(data) {
  return request({
    url: '/api/v1/migration/env/profiles',
    method: 'post',
    data
  })
}

export function deleteMigrationEnvironmentProfile(envKey) {
  return request({
    url: '/api/v1/migration/env/profiles/' + envKey,
    method: 'delete'
  })
}