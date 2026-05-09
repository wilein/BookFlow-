<script lang="ts" setup>
import { RouterView } from 'vue-router';

defineOptions({ name: 'LayoutContent' });
</script>

<template>
  <div class="relative h-full">
    <RouterView v-slot="{ Component, route }">
      <component
        :is="Component"
        v-if="Component && !route.meta.iframeSrc"
        :key="route.fullPath"
      />
    </RouterView>
  </div>
</template>
