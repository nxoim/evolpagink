<script setup lang="ts">
const props = withDefaults(defineProps<{
  text?: string
  privacyPolicy?: string | null
  icon?: string
}>(), {
  text: "We use cookies to improve your experience, analyze traffic, and personalize content.",
  icon: "ph:cookie-bold"
})

const isDismissed = ref(true)

onMounted(() => {
  isDismissed.value = sessionStorage.getItem('cookie-banner-dismissed') === 'true'
})

const dismiss = () => {
  sessionStorage.setItem('cookie-banner-dismissed', 'true')
  isDismissed.value = true
}
</script>

<template>
  <div
    v-if="!isDismissed"
    class="fixed z-50 bottom-4 left-4 right-4 sm:left-auto sm:right-6 sm:bottom-6 sm:max-w-xs
           rounded-xl border border-gray-200/50 bg-white p-4 shadow-xl ring-1 ring-gray-200/30
           dark:bg-zinc-900 dark:border-zinc-800 dark:ring-white/5"
  >
    <div class="flex items-start gap-3">
      <div class="flex shrink-0 items-center justify-center rounded-lg bg-primary-500/10 p-2 text-primary-600 dark:text-primary-400">
        <Icon :name="icon" class="h-5 w-5" />
      </div>

      <div class="flex-1">
        <p class="text-sm leading-relaxed dark:text-zinc-200">
          {{ text }}
        </p>
        <div class="mt-4 flex flex-wrap items-center gap-2">
          <button
            @click="dismiss"
            class="rounded-lg bg-primary-600 px-4 py-1.5 text-xs font-bold text-white hover:bg-primary-700"
          >
            Dismiss
          </button>

          <NuxtLink
            v-if="privacyPolicy"
            :to="privacyPolicy"
            target="_blank"
            class="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-semibold text-gray-600 hover:bg-gray-50 dark:border-zinc-700 dark:text-zinc-300 dark:hover:bg-zinc-800"
          >
            Privacy Policy
            <Icon name="heroicons:arrow-top-right-on-square-20-solid" class="h-3.5 w-3.5" />
          </NuxtLink>
        </div>
      </div>

      <button
        @click="dismiss"
        class="shrink-0 text-gray-400 hover:text-gray-600 dark:hover:text-zinc-200"
        aria-label="Close"
      >
        <Icon name="heroicons:x-mark-20-solid" class="h-5 w-5" />
      </button>
    </div>
  </div>
</template>