<template>
  <div class="app-container migration-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-title">Genesys Cloud Migration Center</div>
      <div class="hero-subtitle">Compare, validate, approve, and publish migrations with audit trails.</div>
    </el-card>

    <el-card shadow="hover" class="panel-card mb16">
      <div slot="header" class="card-header between">
        <span>Environment Profiles</span>
        <span class="muted">One-time setup for region, client_id, and client_secret</span>
      </div>
      <el-row :gutter="16">
        <el-col :span="14">
          <el-table :data="environmentProfiles" border stripe height="220" @row-click="selectProfileFromRow">
            <el-table-column prop="envKey" label="Env Key" width="140" />
            <el-table-column prop="displayName" label="Display Name" min-width="180" />
            <el-table-column prop="region" label="Region" width="140" />
            <el-table-column prop="enabled" label="Enabled" width="100">
              <template slot-scope="scope">
                <el-tag :type="scope.row.enabled ? 'success' : 'info'" size="mini">{{ scope.row.enabled ? 'Yes' : 'No' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :span="10">
          <el-form label-width="110px" size="small" class="profile-form">
            <el-form-item label="Env Key">
              <el-input v-model="profileForm.envKey" placeholder="venv-a" />
            </el-form-item>
            <el-form-item label="Display Name">
              <el-input v-model="profileForm.displayName" placeholder="Sandbox A" />
            </el-form-item>
            <el-form-item label="Region">
              <el-input v-model="profileForm.region" placeholder="us-east-1" />
            </el-form-item>
            <el-form-item label="Client ID">
              <el-input v-model="profileForm.clientId" placeholder="client id" />
            </el-form-item>
            <el-form-item label="Client Secret">
              <el-input v-model="profileForm.clientSecret" placeholder="client secret" show-password />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="profileForm.enabled">Enabled</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="handleSaveProfile">Save Profile</el-button>
              <el-button @click="loadEnvironmentProfiles">Reload</el-button>
              <el-button type="danger" plain :disabled="!profileForm.envKey" @click="handleDeleteProfile">Delete</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="mb16">
      <el-col :span="8">
        <el-card shadow="hover" class="panel-card">
          <div slot="header" class="card-header">Environment</div>
          <el-form label-width="100px" size="small">
            <el-form-item label="Source Env">
              <el-select v-model="form.sourceEnv" filterable placeholder="Select source profile" style="width: 100%">
                <el-option
                  v-for="item in environmentProfiles"
                  :key="'src-' + item.envKey"
                  :label="item.displayName ? item.displayName + ' (' + item.envKey + ')' : item.envKey"
                  :value="item.envKey"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="Target Env">
              <el-select v-model="form.targetEnv" filterable placeholder="Select target profile" style="width: 100%">
                <el-option
                  v-for="item in environmentProfiles"
                  :key="'dst-' + item.envKey"
                  :label="item.displayName ? item.displayName + ' (' + item.envKey + ')' : item.envKey"
                  :value="item.envKey"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="Reviewer Email">
              <el-input v-model="form.email" placeholder="team@example.com" />
            </el-form-item>
            <el-form-item label="Sync Mode">
              <el-select v-model="form.syncMode" style="width: 100%">
                <el-option label="With Dependencies" value="WITH_DEPENDENCIES" />
                <el-option label="Flow Only" value="FLOW_ONLY" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="form.includeDependencies">Include dependency items in migration request</el-checkbox>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="hover" class="panel-card">
          <div slot="header" class="card-header between">
            <span>Resource Selection</span>
            <span class="muted">Select source resources to compare against venv B</span>
          </div>
          <el-table
            ref="resourceTable"
            :data="resourceCatalog"
            border
            stripe
            height="280"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="name" label="Resource Name" min-width="180" />
            <el-table-column prop="type" label="Type" width="120" />
            <el-table-column prop="terraformType" label="Terraform Type" min-width="220" />
            <el-table-column prop="extra" label="Extra" min-width="220" show-overflow-tooltip />
          </el-table>
          <div class="action-bar">
            <el-button type="primary" icon="el-icon-sort" :loading="compareLoading" @click="handleCompare">Compare</el-button>
            <el-button type="success" icon="el-icon-s-promotion" :loading="planLoading" @click="handleCreatePlan">Create Plan</el-button>
            <el-button type="warning" icon="el-icon-message" :loading="passcodeLoading" @click="handleSendPasscode">Send Passcode</el-button>
            <el-button type="danger" icon="el-icon-upload2" :loading="publishLoading" @click="handlePublish">Verify & Publish</el-button>
          </div>
          <div class="meta-line">
            <span>Selected resources: {{ selectedResources.length }}</span>
            <span>Job ID: {{ jobId || '—' }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="Comparison" name="comparison">
        <el-card shadow="hover">
          <el-table :data="compareResult" border stripe>
            <el-table-column prop="resourceName" label="Resource" min-width="180" />
            <el-table-column prop="resourceType" label="Type" width="120" />
            <el-table-column label="Dependencies" min-width="240">
              <template slot-scope="scope">
                <el-tag
                  v-for="(item, index) in scope.row.dependencies || []"
                  :key="index"
                  type="warning"
                  size="mini"
                  class="mr6"
                >
                  {{ item }}
                </el-tag>
                <span v-if="!scope.row.dependencies || !scope.row.dependencies.length">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="sourceValue" label="venv A" min-width="180" />
            <el-table-column prop="targetValue" label="venv B" min-width="180" />
            <el-table-column label="Decision" min-width="260">
              <template slot-scope="scope">
                <el-tag :type="scope.row.hasDependency ? 'warning' : 'success'" size="mini">
                  {{ scope.row.hasDependency ? 'Dependency found' : 'No dependency' }}
                </el-tag>
                <el-tag :type="scope.row.targetExists ? 'info' : 'success'" size="mini" class="ml6">
                  {{ scope.row.targetExists ? 'Target exists' : 'New resource' }}
                </el-tag>
                <div class="recommendation">{{ scope.row.recommendation }}</div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Progress" name="progress">
        <el-card shadow="hover">
          <div class="progress-summary">
            <div>
              <div class="summary-label">Job Status</div>
              <div class="summary-value">{{ progress.jobStatus || '—' }}</div>
            </div>
            <div>
              <div class="summary-label">Progress</div>
              <el-progress :percentage="progress.progressPercent || 0" :stroke-width="18" />
            </div>
            <div>
              <div class="summary-label">Timeout</div>
              <div class="summary-value">{{ progress.timeoutMinutes || 30 }} minutes</div>
            </div>
            <div>
              <div class="summary-label">Passcode</div>
              <div class="summary-value">{{ progress.passcodeVerified ? 'Verified' : 'Pending' }}</div>
            </div>
          </div>
          <el-table :data="progress.progressItems || []" border stripe>
            <el-table-column prop="orderIndex" label="#" width="70" />
            <el-table-column prop="resourceName" label="Resource" min-width="180" />
            <el-table-column prop="resourceType" label="Type" width="120" />
            <el-table-column prop="status" label="Status" width="140">
              <template slot-scope="scope">
                <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="elapsed" label="Elapsed" width="110" />
            <el-table-column prop="message" label="Message" min-width="220" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Passcode" name="passcode">
        <el-card shadow="hover">
          <el-form label-width="120px" size="small" class="passcode-form">
            <el-form-item label="Job ID">
              <el-input v-model="jobId" placeholder="plan job id" />
            </el-form-item>
            <el-form-item label="Passcode">
              <el-input v-model="form.passcode" placeholder="6-digit code" />
            </el-form-item>
            <el-form-item label="Reviewer Email">
              <el-input v-model="form.email" placeholder="team@example.com" />
            </el-form-item>
          </el-form>
          <div class="action-bar">
            <el-button type="warning" icon="el-icon-message" :loading="passcodeLoading" @click="handleSendPasscode">Send Passcode</el-button>
            <el-button type="primary" icon="el-icon-check" @click="handleVerifyPasscode">Verify Passcode</el-button>
            <el-button type="danger" icon="el-icon-upload2" :loading="publishLoading" @click="handlePublish">Publish</el-button>
          </div>
          <el-alert
            v-if="passcodeDebug"
            title="Local debug code is shown below for development only"
            type="warning"
            :description="passcodeDebug"
            show-icon
            class="mt12"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Audit Log" name="audit">
        <el-card shadow="hover">
          <div class="action-bar">
            <el-button type="primary" icon="el-icon-refresh" @click="loadAuditLog">Refresh Audit Log</el-button>
          </div>
          <el-table :data="auditLog" border stripe>
            <el-table-column prop="timestamp" label="Time" width="180" />
            <el-table-column prop="jobId" label="Job ID" min-width="160" />
            <el-table-column prop="action" label="Action" width="160" />
            <el-table-column prop="status" label="Status" width="120" />
            <el-table-column prop="message" label="Message" min-width="220" />
            <el-table-column prop="detail" label="Detail" min-width="220" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import {
  compareMigrationResources,
  createMigrationPlan,
  getMigrationProgress,
  listMigrationEnvironmentProfiles,
  listMigrationAuditLog,
  publishMigrationJob,
  sendMigrationPasscode,
  saveMigrationEnvironmentProfile,
  deleteMigrationEnvironmentProfile,
  verifyMigrationPasscode
} from '@/api/migration'

export default {
  name: 'MigrationCenter',
  data() {
    return {
      activeTab: 'comparison',
      selectedResources: [],
      jobId: '',
      compareLoading: false,
      planLoading: false,
      passcodeLoading: false,
      publishLoading: false,
      profileSaving: false,
      pollTimer: null,
      passcodeDebug: '',
      environmentProfiles: [],
      profileForm: {
        envKey: '',
        displayName: '',
        region: 'us-east-1',
        clientId: '',
        clientSecret: '',
        enabled: true
      },
      form: {
        sourceEnv: '',
        targetEnv: '',
        email: 'review@example.com',
        syncMode: 'WITH_DEPENDENCIES',
        includeDependencies: true,
        passcode: ''
      },
      resourceCatalog: [
        { id: 'flow-001', name: 'inbound_flow_main', type: 'FLOW', terraformType: 'genesyscloud_architect_flow', extra: 'timeout=20;queue=general_support;prompt=welcome_prompt' },
        { id: 'queue-001', name: 'general_support', type: 'QUEUE', terraformType: 'genesyscloud_routing_queue', extra: 'media=voice' },
        { id: 'prompt-001', name: 'welcome_prompt', type: 'PROMPT', terraformType: 'genesyscloud_architect_user_prompt', extra: 'welcome message' },
        { id: 'user-001', name: 'jane.doe', type: 'USER', terraformType: 'genesyscloud_user', extra: 'role=agent' },
        { id: 'role-001', name: 'contact_center_admin', type: 'ROLE', terraformType: 'genesyscloud_authorization_role', extra: 'permissions=full' }
      ],
      compareResult: [],
      progress: {
        progressPercent: 0,
        jobStatus: '',
        timeoutMinutes: 30,
        progressItems: []
      },
      auditLog: []
    }
  },
  mounted() {
    this.loadEnvironmentProfiles()
    this.loadAuditLog()
  },
  beforeDestroy() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer)
      this.pollTimer = null
    }
  },
  methods: {
    async loadEnvironmentProfiles() {
      const response = await listMigrationEnvironmentProfiles()
      this.environmentProfiles = response.data || []
      if (!this.form.sourceEnv && this.environmentProfiles.length > 0) {
        this.form.sourceEnv = this.environmentProfiles[0].envKey
      }
      if (!this.form.targetEnv && this.environmentProfiles.length > 1) {
        this.form.targetEnv = this.environmentProfiles[1].envKey
      } else if (!this.form.targetEnv && this.environmentProfiles.length > 0) {
        this.form.targetEnv = this.environmentProfiles[0].envKey
      }
      if (!this.profileForm.envKey && this.environmentProfiles.length > 0) {
        this.profileForm.envKey = this.environmentProfiles[0].envKey
      }
    },
    selectProfileFromRow(row) {
      if (!row || !row.envKey) {
        return
      }
      this.profileForm = {
        envKey: row.envKey,
        displayName: row.displayName || '',
        region: row.region || 'us-east-1',
        clientId: row.clientId || '',
        clientSecret: '',
        enabled: row.enabled !== false
      }
    },
    async handleSaveProfile() {
      if (!this.profileForm.envKey) {
        this.$message.warning('Please input env key')
        return
      }
      this.profileSaving = true
      try {
        await saveMigrationEnvironmentProfile(this.profileForm)
        this.$message.success('Profile saved')
        await this.loadEnvironmentProfiles()
      } finally {
        this.profileSaving = false
      }
    },
    async handleDeleteProfile() {
      if (!this.profileForm.envKey) {
        return
      }
      await this.$confirm(`Delete profile ${this.profileForm.envKey}?`, 'Confirm', {
        type: 'warning'
      })
      await deleteMigrationEnvironmentProfile(this.profileForm.envKey)
      this.$message.success('Profile deleted')
      this.profileForm = {
        envKey: '',
        displayName: '',
        region: 'us-east-1',
        clientId: '',
        clientSecret: '',
        enabled: true
      }
      await this.loadEnvironmentProfiles()
    },
    handleSelectionChange(rows) {
      this.selectedResources = rows
    },
    async handleCompare() {
      if (!this.selectedResources.length) {
        this.$message.warning('Please select at least one resource')
        return
      }
      this.compareLoading = true
      try {
        const response = await compareMigrationResources({
          sourceEnv: this.form.sourceEnv,
          targetEnv: this.form.targetEnv,
          selectedResources: this.selectedResources
        })
        this.compareResult = response.data || []
        this.activeTab = 'comparison'
        this.$message.success('Comparison completed')
        this.loadAuditLog()
      } finally {
        this.compareLoading = false
      }
    },
    async handleCreatePlan() {
      if (!this.selectedResources.length) {
        this.$message.warning('Please select at least one resource')
        return
      }
      this.planLoading = true
      try {
        const response = await createMigrationPlan({
          sourceEnv: this.form.sourceEnv,
          targetEnv: this.form.targetEnv,
          selectedResources: this.selectedResources,
          syncMode: this.form.syncMode
        })
        const job = response.data || {}
        this.jobId = job.jobId || ''
        this.$message.success('Plan created')
        await this.refreshProgress()
        await this.loadAuditLog()
      } finally {
        this.planLoading = false
      }
    },
    async handleSendPasscode() {
      if (!this.jobId) {
        await this.handleCreatePlan()
        if (!this.jobId) {
          return
        }
      }
      this.passcodeLoading = true
      try {
        const response = await sendMigrationPasscode({
          jobId: this.jobId,
          email: this.form.email
        })
        const payload = response.data || {}
        this.passcodeDebug = payload.debugPasscode ? `Debug passcode: ${payload.debugPasscode}` : 'Passcode sent'
        this.$message.success('Passcode sent')
        await this.loadAuditLog()
      } finally {
        this.passcodeLoading = false
      }
    },
    async handleVerifyPasscode() {
      if (!this.jobId) {
        this.$message.warning('Please create a plan job first')
        return
      }
      if (!this.form.passcode) {
        this.$message.warning('Please input passcode')
        return
      }
      try {
        await verifyMigrationPasscode({
          jobId: this.jobId,
          passcode: this.form.passcode
        })
        this.$message.success('Passcode verified')
        await this.refreshProgress()
        await this.loadAuditLog()
      } catch (error) {
        this.$message.error(error.message || 'Verification failed')
      }
    },
    async handlePublish() {
      if (!this.jobId) {
        this.$message.warning('Please create a plan job first')
        return
      }
      this.publishLoading = true
      try {
        const response = await publishMigrationJob({ jobId: this.jobId })
        const payload = response.data || {}
        this.$message.success(payload.message || 'Publish started')
        this.activeTab = 'progress'
        this.startPolling()
        await this.refreshProgress()
        await this.loadAuditLog()
      } finally {
        this.publishLoading = false
      }
    },
    async refreshProgress() {
      if (!this.jobId) {
        return
      }
      const response = await getMigrationProgress(this.jobId)
      this.progress = response.data || this.progress
    },
    async loadAuditLog() {
      if (!this.jobId) {
        this.auditLog = []
        return
      }
      const response = await listMigrationAuditLog(this.jobId)
      this.auditLog = response.data || []
    },
    startPolling() {
      if (this.pollTimer) {
        return
      }
      this.pollTimer = setInterval(() => {
        this.refreshProgress().catch(() => {})
      }, 2000)
    },
    statusTagType(status) {
      if (status === 'SUCCESS') {
        return 'success'
      }
      if (status === 'PROCESSING') {
        return 'warning'
      }
      if (status === 'FAILED') {
        return 'danger'
      }
      return 'info'
    }
  }
}
</script>

<style scoped>
.migration-page {
  background: linear-gradient(180deg, #0f172a 0%, #111827 35%, #f8fafc 35%, #f8fafc 100%);
  min-height: calc(100vh - 84px);
}

.hero-card {
  background: linear-gradient(135deg, #111827 0%, #1e3a8a 55%, #0369a1 100%);
  color: #f8fafc;
  border: none;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.hero-subtitle {
  margin-top: 6px;
  opacity: 0.88;
}

.panel-card,
.el-card {
  border-radius: 14px;
}

.card-header,
.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.muted {
  color: #94a3b8;
  font-size: 12px;
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.meta-line {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  color: #64748b;
  font-size: 12px;
}

.recommendation {
  margin-top: 6px;
  color: #334155;
  font-size: 12px;
}

.progress-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.summary-label {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.summary-value {
  font-weight: 600;
  color: #0f172a;
}

.mr6 {
  margin-right: 6px;
  margin-bottom: 4px;
}

.ml6 {
  margin-left: 6px;
}

.mt12 {
  margin-top: 12px;
}

.mb16 {
  margin-bottom: 16px;
}

.passcode-form {
  max-width: 600px;
}

@media (max-width: 1200px) {
  .progress-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .progress-summary {
    grid-template-columns: 1fr;
  }
}
</style>