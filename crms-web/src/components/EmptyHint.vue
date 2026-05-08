<template>
  <div class="empty-hint">
    <svg class="empty-hint__icon" viewBox="0 0 120 120" fill="none" aria-hidden="true">
      <!-- 一个轻量的"文件夹 + 折角"业务感插图，不依赖 element-plus 默认黑白人形图 -->
      <defs>
        <linearGradient id="empty-hint-bg" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stop-color="#EEF4FF" />
          <stop offset="100%" stop-color="#E5EDFF" />
        </linearGradient>
      </defs>
      <circle cx="60" cy="60" r="54" fill="url(#empty-hint-bg)" />
      <path
        d="M36 46 H56 L62 52 H84 V82 H36 Z"
        fill="#FFFFFF"
        stroke="#A8BFFF"
        stroke-width="1.5"
        stroke-linejoin="round"
      />
      <path d="M36 46 H56 L62 52 H84" fill="none" stroke="#7C9AFF" stroke-width="1.5" />
      <path d="M44 64 H76 M44 71 H68" stroke="#A8BFFF" stroke-width="2" stroke-linecap="round" />
    </svg>
    <div class="empty-hint__desc">{{ description }}</div>
    <div v-if="$slots.default" class="empty-hint__extra">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ description?: string }>()
</script>

<style scoped lang="scss">
.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;

  &__icon {
    width: 88px;
    height: 88px;
    margin-bottom: 12px;
  }

  &__desc {
    color: #606266;
  }

  &__extra {
    margin-top: 10px;
  }
}
</style>
