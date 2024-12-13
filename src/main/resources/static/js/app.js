class UserService {
  async getUserInfo() {
    if (!auth.isAuthenticated()) {
      window.location.href = '/login';
      return;
    }

    try {
      const response = await fetch('/api/user/info', {
        headers: auth.getAuthHeaders()
      });

      if (!response.ok) {
        if (response.status === 401) {
          auth.removeToken();
          window.location.href = '/login';
          throw new Error('Unauthorized');
        }
        throw new Error('Network response was not ok');
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching user info:', error);
      throw error;
    }
  }
}

class UIManager {
  constructor() {
    this.userService = new UserService();
  }

  updateUI(userData) {
    // 更新个人资料卡片
    const pictureHtml = userData.picture
        ? `<img src="${userData.picture}" class="rounded-circle" style="width: 150px; height: 150px; object-fit: cover;" alt="Profile Picture">`
        : `<div class="rounded-circle bg-secondary d-flex align-items-center justify-content-center mx-auto" style="width: 150px; height: 150px;">
                <span class="text-white" style="font-size: 3em;">${userData.name.charAt(0)}</span>
               </div>`;
    document.getElementById('profilePicture').innerHTML = pictureHtml;

    // 更新用户基本信息
    document.getElementById('userName').textContent = userData.name;
    document.getElementById('userEmail').textContent = userData.email;
    document.getElementById('userProvider').textContent = userData.provider || 'Local';

    // 更新详细信息表格
    document.getElementById('tableUserName').textContent = userData.name;
    document.getElementById('tableUserEmail').textContent = userData.email;
    document.getElementById('tableUserProvider').textContent = userData.provider || 'Local';

  }

  async refreshUserInfo() {
    try {
      const userData = await this.userService.getUserInfo();
      this.updateUI(userData);
    } catch (error) {
      if (error.message !== 'Unauthorized') {
        alert('Failed to load user information');
      }
    }
  }

  init() {
    // 初始化事件监听
    document.getElementById('refreshButton')?.addEventListener('click',
        () => this.refreshUserInfo());
    document.getElementById('logoutButton')?.addEventListener('click',
        () => auth.logout());

    // 初始加载用户信息
    this.refreshUserInfo();
  }
}

// 当文档加载完成时初始化
document.addEventListener('DOMContentLoaded', () => {
  const ui = new UIManager();
  ui.init();
});