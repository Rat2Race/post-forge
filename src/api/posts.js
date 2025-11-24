import apiClient from './axios';

export const postsAPI = {
  // 게시글 목록 조회
  getPosts: async (page = 0, size = 20) => {
    const response = await apiClient.get('/posts', {
      params: { page, size, sort: 'createdAt,desc' },
    });
    return response.data;
  },

  // 게시글 상세 조회
  getPost: async (postId) => {
    const response = await apiClient.get(`/posts/${postId}`);
    return response.data;
  },

  // 게시글 생성
  createPost: async (data) => {
    const response = await apiClient.post('/posts', {
      title: data.title,
      content: data.content,
    });
    return response.data;
  },

  // 게시글 수정
  updatePost: async (postId, data) => {
    const response = await apiClient.put(`/posts/${postId}`, {
      title: data.title,
      content: data.content,
    });
    return response.data;
  },

  // 게시글 삭제
  deletePost: async (postId) => {
    const response = await apiClient.delete(`/posts/${postId}`);
    return response.data;
  },

  // 게시글 좋아요 토글
  toggleLike: async (postId) => {
    const response = await apiClient.post(`/posts/${postId}/like`);
    return response.data;
  },
};
