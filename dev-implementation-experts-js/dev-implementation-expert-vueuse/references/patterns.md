# VueUse Patterns & Best Practices in Nuxt 3

## Setup

```bash
pnpm add @vueuse/nuxt @vueuse/core
```

`nuxt.config.ts`:
```typescript
export default defineNuxtConfig({
  modules: ['@vueuse/nuxt'],
})
```

## Auto-Import Conflicts

`@vueuse/nuxt` **disables** auto-import for these — they conflict with Nuxt built-ins:

| Composable | Nuxt Built-in | Fix |
|---|---|---|
| `useFetch` | Nitro/OFetch data fetching | Import explicitly from `@vueuse/core` |
| `useHead` | Unhead tag management | Import explicitly from `@vueuse/core` |
| `useCookie` | Nuxt cookie management | Import explicitly from `@vueuse/core` |
| `useStorage` | (reserved) | Import explicitly from `@vueuse/core` |
| `useImage` | Nuxt Image | Import explicitly from `@vueuse/core` |
| `toRef`, `toRefs`, `toValue` | Vue Core | Import explicitly from `@vueuse/core` |

```typescript
// Explicit import for conflicting composables
import { useStorage, useFetch } from '@vueuse/core'
```

## Composable Reference by Category

### 🖱️ Sensors & Input
| Composable | Description |
|---|---|
| `useMouse` | Reactive mouse position (x, y) |
| `useMouseInElement` | Mouse position relative to element |
| `useMousePressed` | Mouse pressing state |
| `useScroll` | Scroll position and state |
| `useInfiniteScroll` | Infinite scrolling logic |
| `useMagicKeys` | Reactive keyboard shortcuts |
| `useGeolocation` | Reactive Geolocation API |
| `useNetwork` | Reactive network status |
| `useOnline` | Reactive online state |
| `useDeviceMotion` | Reactive DeviceMotion |
| `useDeviceOrientation` | Reactive DeviceOrientation |
| `useBattery` | Reactive Battery Status API |
| `useTextSelection` | Reactive text selection |

### 🌐 Browser
| Composable | Description |
|---|---|
| `useDark` | Dark mode with auto-detection |
| `usePreferredDark` | Dark mode preference |
| `useColorMode` | Color mode (dark/light/sepia) |
| `useClipboard` | Clipboard API (copy/paste) |
| `useFullscreen` | Fullscreen API |
| `useTitle` | Reactive document title |
| `useFavicon` | Reactive favicon |
| `useBreakpoints` | Reactive viewport breakpoints |
| `useMediaQuery` | Reactive Media Query |
| `usePermission` | Permissions API |
| `useShare` | Web Share API |
| `useWakeLock` | Screen Wake Lock API |
| `useMediaControls` | Video/audio media controls |

### 💾 State & Storage
| Composable | Description |
|---|---|
| `useLocalStorage` | Reactive `localStorage` |
| `useSessionStorage` | Reactive `sessionStorage` |
| `useStorageAsync` | Async storage (indexedDB, etc.) |
| `useRefHistory` | Ref change history (undo/redo) |
| `useManualRefHistory` | Manual history tracking |
| `useDebouncedRef` | Ref with debounced updates |
| `useThrottledRef` | Ref with throttled updates |
| `createGlobalState` | State shared across component instances |
| `useLastChanged` | Timestamp of last change |

### 🧱 Elements (DOM)
| Composable | Description |
|---|---|
| `useElementSize` | Reactive element size |
| `useElementBounding` | Reactive bounding box |
| `useElementVisibility` | Viewport visibility tracking |
| `useDraggable` | Make elements draggable |
| `useResizeObserver` | Watch element size changes |
| `useIntersectionObserver` | Watch element intersection |
| `useMutationObserver` | Watch DOM mutations |
| `useWindowSize` | Reactive window size |
| `useWindowFocus` | Window focus state |
| `useWindowScroll` | Reactive window scroll |
| `useActiveElement` | Reactive `document.activeElement` |
| `onClickOutside` | Clicks outside an element |
| `onLongPress` | Long press gestures |

### ⏳ Time
| Composable | Description |
|---|---|
| `useNow` | Reactive current timestamp |
| `useDateFormat` | Reactive time formatting |
| `useTimeAgo` | Reactive "time ago" string |
| `useIntervalFn` | `setInterval` wrapper with controls |
| `useTimeoutFn` | `setTimeout` wrapper with controls |
| `useTimestamp` | Reactive timestamp |

### 🛠️ Utilities
| Composable | Description |
|---|---|
| `useToggle` | Boolean switcher |
| `useCounter` | Counter with utility functions |
| `useAsyncState` | Reactive async state (loading/error/data) |
| `useCloned` | Reactive clone of a value |
| `useCycleList` | Cycle through a list |
| `useConfirmDialog` | Confirm dialog hooks |
| `useEventBus` | Basic event bus |
| `useVModel` | v-model binding helper |
| `useVModels` | Multiple v-model bindings |
| `useBase64` | Reactive Base64 generation |
| `useStepper` | Stepper helper |

### 🎬 Animation
| Composable | Description |
|---|---|
| `useTransition` | Transition between values |
| `useRafFn` | `requestAnimationFrame` wrapper |
| `useInterval` | Reactive interval |

### ⚠️ Watch & Reactivity Extensions
| Composable | Description |
|---|---|
| `watchOnce` | `watch` that triggers once |
| `watchDebounced` | Debounced `watch` |
| `watchThrottled` | Throttled `watch` |
| `watchPausable` | Pausable `watch` |
| `whenever` | `watch(source, cb)` only when truthy |
| `until` | Promise-based wait for condition |