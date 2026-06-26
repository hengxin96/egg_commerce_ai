<template>
  <div>
    <svg-icon icon-class="question" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'EggDoc',
  data() {
    return {
      url: 'http://doc.egg.vip/egg-vue'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>