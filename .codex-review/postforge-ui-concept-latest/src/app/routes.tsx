import { createBrowserRouter } from "react-router";
import { RootLayout } from "./layouts/root-layout";
import { HomePage } from "./pages/home";
import { LoginPage } from "./pages/login";
import { RegisterPage } from "./pages/register";
import { OAuthCallbackPage } from "./pages/oauth-callback";
import { PostDetailPage } from "./pages/post-detail";
import { NewPostPage } from "./pages/new-post";
import { EditPostPage } from "./pages/edit-post";
import { ProfilePage } from "./pages/profile";
import { AIChatPage } from "./pages/ai-chat";
import { AIGeneratePage } from "./pages/ai-generate";
import { NotFoundPage } from "./pages/not-found";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: RootLayout,
    children: [
      { index: true, Component: HomePage },
      { path: "login", Component: LoginPage },
      { path: "register", Component: RegisterPage },
      { path: "oauth2/callback", Component: OAuthCallbackPage },
      { path: "posts/:id", Component: PostDetailPage },
      { path: "posts/new", Component: NewPostPage },
      { path: "posts/:id/edit", Component: EditPostPage },
      { path: "profile", Component: ProfilePage },
      { path: "ai/chat", Component: AIChatPage },
      { path: "ai/generate", Component: AIGeneratePage },
      { path: "*", Component: NotFoundPage },
    ],
  },
]);
