<template>
  <div class="sample-container">
    <div class="left-section">
      <h1 class="text-5xl font-bold">evolpagink docs</h1>
      <p class="text-lg text-gray-500">Kotlin Multiplatform pagination library. Get started or explore the Compose WasmJS sample.</p>
      
      <div class="flex gap-3 flex-wrap mt-4">
        <UButton 
          color="neutral" 
          size="xl" 
          to="/quick-start/installation" 
          trailing-icon="i-lucide-arrow-right"
        >
          Quick Start
        </UButton>
        
        <UButton 
          color="neutral" 
          icon="i-lucide-code" 
          size="xl" 
          to="/other/dokka/index.html" 
          variant="outline"
          rel="noopener noreferrer"
        >
          API Reference
        </UButton>
        
        <UButton 
          v-if="!showSample" 
          color="neutral" 
          icon="i-lucide-eye" 
          size="xl" 
          to="/other/sample/index.html" 
          variant="outline"
          target="_blank"
          rel="noopener noreferrer"
        >
          Sample
        </UButton>
      </div>
    </div>

    <div v-if="showSample" class="right-section">
      <iframe 
        src="/other/sample/index.html" 
        class="w-full h-full border border-accented rounded-xl shadow-lg" 
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useWindowSize } from '@vueuse/core'


const { width } = useWindowSize()
const showSample = computed(() => width.value >= 1024)
</script>

<style scoped>
.sample-container {
  display: flex;
  flex-direction: row;
  gap: 32px;
  margin: 32px;
  height: 600px;
}

.left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 0 24px;
}

.right-section {
  flex: 1;
  height: 600px;
  overflow: hidden;
}

.left-section h1 {
  margin-bottom: 8px;
}

.left-section p {
  margin-bottom: 24px;
}

@media (max-width: 1023px) {
  .sample-container {
    flex-direction: column;
    height: auto;
    min-height: 600px;
  }
  
  .left-section,
  .right-section {
    width: 100%;
  }
  
  .right-section {
    margin-top: 32px;
  }
}

.right-section iframe {
  display: block;
}

.right-section:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}
</style>
