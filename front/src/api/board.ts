import { http, baseUrls } from './http';
import type {
  PostResponse,
  PostSummaryResponse,
  PostRequest,
  CommentResponse,
  CommentSummaryResponse,
  CommentRequest,
  LikeResponse,
  Page
} from './types';

// Posts
export async function listPosts(page: number = 0, size: number = 20): Promise<Page<PostResponse>> {
  return http.get(baseUrls.BOARD_BASE, `/api/posts?page=${page}&size=${size}&sort=createdAt,desc`);
}

export async function getPost(id: number): Promise<PostResponse> {
  return http.get(baseUrls.BOARD_BASE, `/api/posts/${id}`);
}

export async function createPost(dto: PostRequest): Promise<PostSummaryResponse> {
  return http.post(baseUrls.BOARD_BASE, '/api/posts', dto);
}

export async function updatePost(id: number, dto: PostRequest): Promise<PostSummaryResponse> {
  return http.put(baseUrls.BOARD_BASE, `/api/posts/${id}`, dto);
}

export async function deletePost(id: number): Promise<string> {
  return http.delete(baseUrls.BOARD_BASE, `/api/posts/${id}`);
}

export async function togglePostLike(postId: number): Promise<LikeResponse> {
  return http.post(baseUrls.BOARD_BASE, `/api/posts/${postId}/like`);
}

// Comments
export async function listComments(postId: number, page: number = 0, size: number = 50): Promise<Page<CommentResponse>> {
  return http.get(baseUrls.BOARD_BASE, `/api/posts/${postId}/comments?page=${page}&size=${size}&sort=createdAt,asc`);
}

export async function createComment(postId: number, dto: CommentRequest): Promise<CommentSummaryResponse> {
  return http.post(baseUrls.BOARD_BASE, `/api/posts/${postId}/comments`, dto);
}

export async function updateComment(postId: number, commentId: number, dto: CommentRequest): Promise<CommentSummaryResponse> {
  return http.put(baseUrls.BOARD_BASE, `/api/posts/${postId}/comments/${commentId}`, dto);
}

export async function deleteComment(postId: number, commentId: number): Promise<string> {
  return http.delete(baseUrls.BOARD_BASE, `/api/posts/${postId}/comments/${commentId}`);
}

export async function toggleCommentLike(postId: number, commentId: number): Promise<LikeResponse> {
  return http.post(baseUrls.BOARD_BASE, `/api/posts/${postId}/comments/${commentId}/like`);
}
