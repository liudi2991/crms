<template>
  <div class="crms-filter">
    <el-input
      v-model="query.keyword"
      placeholder="搜索客户名称 / 编号 / 统一信用代码"
      clearable
      :prefix-icon="Search"
      style="width: 280px"
      @keyup.enter="$emit('search')"
    />
    <el-select v-model="query.type" placeholder="类型" clearable style="width: 120px">
      <el-option
        v-for="(label, key) in CustomerType"
        :key="key"
        :label="label"
        :value="key"
      />
    </el-select>
    <el-select v-model="query.level" placeholder="等级" clearable style="width: 100px">
      <el-option v-for="l in CustomerLevel" :key="l" :label="l" :value="l" />
    </el-select>
    <el-select v-model="query.status" placeholder="状态" clearable style="width: 100px">
      <el-option label="启用" value="ACTIVE" />
      <el-option label="停用" value="DISABLED" />
    </el-select>
    <el-button type="primary" :icon="Search" @click="$emit('search')">查询</el-button>
    <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
  </div>
</template>

<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue'
import { CustomerLevel, CustomerType } from '@/utils/enum'
import type { CustomerQuery } from '@/api/customer'

defineProps<{ query: CustomerQuery }>()
defineEmits<{ search: []; reset: [] }>()
</script>
