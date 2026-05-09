<script>
import { checkAuthSession } from './utils/api/user';
import { clearSession, hasValidSession } from './utils/auth';

export default {
  onLaunch() {
    this.bootstrapAuth();
  },
  onShow() {
    this.bootstrapAuth();
  },
  methods: {
    async bootstrapAuth() {
      if (!hasValidSession()) {
        clearSession();
        return;
      }
      try {
        await checkAuthSession();
      } catch (error) {
        console.error('bootstrapAuth failed', error);
      }
    }
  }
};
</script>

<style>
/* 每个页面公共css */
</style>
