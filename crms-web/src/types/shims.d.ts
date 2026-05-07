// 第三方包缺类型声明的 shim
// element-plus 的 locale 子路径只导出 .mjs，未带 .d.ts，
// 使用 any 让 ElementPlus.use() 的形参 ConfigProviderProps['locale'] 自洽
declare module 'element-plus/dist/locale/zh-cn.mjs' {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const value: any
  export default value
}

// .vue 单文件组件的全局声明（保险，部分工具链需要）
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
