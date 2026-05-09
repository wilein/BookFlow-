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
page {
  background: #eef3fb;
  color: #172033;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Helvetica, Arial, sans-serif;
}

view,
text,
input,
textarea,
button,
picker {
  box-sizing: border-box;
}

image {
  display: block;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
