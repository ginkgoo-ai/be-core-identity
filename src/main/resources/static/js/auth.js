// src/main/resources/static/js/auth.js

class AuthService {
  constructor() {
    this.tokenKey = 'jwt_token';
  }

  init() {
    // 页面加载时检查token
    const tempToken = this.getTokenFromCookie();
    if (tempToken) {
      this.setToken(tempToken);
    }

    if (!this.isAuthenticated()) {
      window.location.href = '/login';
      return false;
    }
    return true;
  }

  getTokenFromCookie() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
      const [name, value] = cookie.trim().split('=');
      if (name === 'temp_token') {
        // 删除临时cookie
        document.cookie = 'temp_token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        return value;
      }
    }
    return null;
  }

  getToken() {
    return localStorage.getItem(this.tokenKey);
  }

  setToken(token) {
    localStorage.setItem(this.tokenKey, token);
  }

  removeToken() {
    localStorage.removeItem(this.tokenKey);
  }

  isAuthenticated() {
    return !!this.getToken();
  }

  logout() {
    this.removeToken();
    window.location.href = '/login';
  }

  // 获取请求头
  getAuthHeaders() {
    return {
      'Authorization': `Bearer ${this.getToken()}`,
      'Content-Type': 'application/json'
    };
  }

  // 处理API响应的通用方法
  handleApiResponse(response) {
    if (!response.ok) {
      if (response.status === 401) {
        this.removeToken();
        window.location.href = '/login';
        throw new Error('Unauthorized');
      }
      throw new Error('Network response was not ok');
    }
    return response.json();
  }

  // 封装API调用
  async fetchWithAuth(url, options = {}) {
    if (!this.isAuthenticated()) {
      window.location.href = '/login';
      return;
    }

    const defaultOptions = {
      headers: this.getAuthHeaders()
    };

    try {
      const response = await fetch(url, { ...defaultOptions, ...options });
      return this.handleApiResponse(response);
    } catch (error) {
      console.error('API call failed:', error);
      throw error;
    }
  }
}

// 创建全局单例
const auth = new AuthService();

// 在页面加载时进行初始化检查
document.addEventListener('DOMContentLoaded', () => {
  if (auth.init()) {
    // token有效，可以继续加载页面内容
    if (typeof initPage === 'function') {
      initPage();
    }
  }
});