<template>
  <el-card class="chart-card" shadow="hover" :body-style="{ padding: '16px' }">
    <div class="chart-card__head">
      <span class="chart-card__title">{{ title }}</span>
      <div class="chart-card__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="chart-card__body" :style="bodyStyle">
      <slot />
      <div v-if="loading" class="chart-card__mask">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else-if="isEmpty" class="chart-card__empty">
        <el-empty description="暂无数据" :image-size="60" />
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  loading: { type: Boolean, default: false },
  isEmpty: { type: Boolean, default: false },
  height: { type: [Number, String], default: 300 }
})

const bodyStyle = computed(() => ({
  height: typeof props.height === 'number' ? `${props.height}px` : props.height
}))
</script>

<style lang="scss" scoped>
.chart-card {
  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 10px;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  &__body {
    position: relative;
    width: 100%;
  }

  &__mask,
  &__empty {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.9);
    z-index: 2;
  }
}
</style>
