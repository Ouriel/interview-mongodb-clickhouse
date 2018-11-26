import * as dayjs from 'dayjs'

export default {
  now: null,

  init() {
    this.now = dayjs()
  },

  increment() {
    this.now = this.now.add(1, 'day')
  },
}
