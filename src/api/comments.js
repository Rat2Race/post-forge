import apiClient from './axios';

export const commentsAPI = {
  // 댓글 목록 조회
  getComments: async (postId, page = 0, size = 50) => {
    const response = await apiClient.get(`/posts/${postId}/comments`, {
      params: { page, size, sort: 'createdAt,asc' },
    });
    return response.data;
  },

  // 댓글 작성
  createComment: async (postId, data) => {
    const response = await apiClient.post(`/posts/${postId}/comments`, {
      parentId: data.parentId || null,
      content: data.content,
    });
    return response.data;
  },

  // 댓글 수정
  updateComment: async (postId, commentId, content) => {
    const response = await apiClient.put(`/posts/${postId}/comments/${commentId}`, {
      content,
    });
    return response.data;
  },

  // 댓글 삭제
  deleteComment: async (postId, commentId) => {
    const response = await apiClient.delete(`/posts/${postId}/comments/${commentId}`);
    return response.data;
  },

  // 댓글 좋아요 토글
  toggleLike: async (postId, commentId) => {
    const response = await apiClient.post(`/posts/${postId}/comments/${commentId}/like`);
    return response.data;
  },
};
