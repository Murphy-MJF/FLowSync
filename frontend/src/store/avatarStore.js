import { ref } from 'vue'

// 共享头像状态（跨组件实时更新）
export const avatarImage = ref(sessionStorage.getItem('avatarData') || '')
export const avatarColor = ref(sessionStorage.getItem('avatarColor') || '')

export function updateAvatar(image, color) {
  if (image) { avatarImage.value = image; sessionStorage.setItem('avatarData', image) }
  else { avatarImage.value = ''; sessionStorage.removeItem('avatarData') }
  if (color) { avatarColor.value = color; sessionStorage.setItem('avatarColor', color) }
}
