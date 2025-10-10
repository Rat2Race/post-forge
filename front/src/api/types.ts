// Auth
export type TokenResponse = {
  grantType: string;
  accessToken: string;
  refreshToken: string;
};

export type LoginRequest = { id: string; pw: string };
export type SignupRequest = { name: string; id: string; pw: string };

export type MemberResponse = {
  id: number;
  name: string;
  userId: string;
  roles: string[];
};

// Board
export type ArticleDto = {
  id?: number;
  title: string;
  content: string;
  createAt?: string;
  createBy?: string;
  modifiedAt?: string;
  modifiedBy?: string;
};

