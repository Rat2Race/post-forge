// Auth
export type TokenResponse = {
  grantType: string;
  accessToken: string;
  refreshToken: string;
};

export type LoginRequest = { id: string; pw: string };
export type SignupRequest = {
  name: string;
  id: string;
  pw: string;
  email: string;
  nickname: string;
};

export type MemberResponse = {
  id: number;
  name: string;
  userId: string;
  roles: string[];
};

// Board
export type PostResponse = {
  id: number;
  title: string;
  content: string;
  userId: string;
  views: number;
  commentCount: number;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  modifiedAt: string;
};

export type PostSummaryResponse = {
  id: number;
  title: string;
  content: string;
  userId: string;
  createdAt: string;
  modifiedAt: string;
};

export type PostRequest = {
  title: string;
  content: string;
};

export type CommentResponse = {
  id: number;
  content: string;
  userId: string;
  parentId: number | null;
  replyCount: number;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
  modifiedAt: string;
};

export type CommentSummaryResponse = {
  id: number;
  content: string;
  userId: string;
  parentId: number | null;
  createdAt: string;
  modifiedAt: string;
};

export type CommentRequest = {
  content: string;
  parentId?: number | null;
};

export type LikeResponse = {
  isLiked: boolean;
  likeCount: number;
};

export type Page<T> = {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
};

