import { defineConfig } from 'vite'
import typescript from '@rollup/plugin-typescript'
import cssInjectedByJsPlugin from 'vite-plugin-css-injected-by-js'
import * as path from 'path'

export default defineConfig(() => {
  const name = 'emr-editor'
  return {
    plugins: [
      cssInjectedByJsPlugin({
        styleId: `${name}-style`,
        topExecutionPriority: true
      })
    ],
    build: {
      lib: {
        name,
        fileName: name,
        entry: path.resolve(__dirname, 'src/editor/index.ts')
      },
      rollupOptions: {
        external: ['vue'],
        output: {}
      }
    }
  }
})

