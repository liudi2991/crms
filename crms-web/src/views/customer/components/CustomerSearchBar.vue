<template>
  <el-form :model="query" inline @submit.prevent>
    <el-form-item label="关键词">
      <el-input
        v-model="query.keyword"
        placeholder="名称 / 编号 / 统一信用代码"
        clearable
        style="width: 240px"
        @keyup.enter="$emit('search')"
      />
    </el-form-item>
    <el-form-item label="类型">
      <el-select v-model="query.type" placeholder="全部" clearable style="width: 130px">
        <el-option
          v-for="(label, key) in CustomerType"
          :key="key"
          :label="label"
          :value="key"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="等级">
      <el-select v-model="query.level" placeholder="全部" clearable style="width: 100px">
        <el-option v-for="l in CustomerLevel" :key="l" :label="l" :value="l" />
      </el-select>
    </el-form-item>
    <el-form-item label="状态">
      <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
        <el-option label="启用" value="ACTIVE" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="$emit('search')">查询</el-button>
      <el-button @click="$emit('reset')">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { CustomerLevel, CustomerType } from '@/utils/enum'
import type { CustomerQuery } from '@/api/customer'

defineProps<{ query: CustomerQuery }>()
defineEmits<{ search: []; reset: [] }>()
</script>
