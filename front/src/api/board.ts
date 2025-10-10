import { http, baseUrls } from './http';
import type { ArticleDto } from './types';

export async function listArticles(): Promise<ArticleDto[]> {
  return http.get(baseUrls.BOARD_BASE, '/articles/read');
}

export async function getArticle(id: number): Promise<ArticleDto> {
  return http.get(baseUrls.BOARD_BASE, `/articles/read/${id}`);
}

export async function createArticle(dto: ArticleDto): Promise<number> {
  return http.post(baseUrls.BOARD_BASE, '/articles/create', dto);
}

export async function updateArticle(id: number, dto: ArticleDto): Promise<string> {
  return http.put(baseUrls.BOARD_BASE, `/articles/update?id=${id}`, dto);
}

export async function deleteArticle(id: number): Promise<string> {
  return http.delete(baseUrls.BOARD_BASE, `/articles/delete?id=${id}`);
}

export async function createComment(articleId: number): Promise<number> {
  return http.post(baseUrls.BOARD_BASE, `/comments/create/${articleId}`);
}

