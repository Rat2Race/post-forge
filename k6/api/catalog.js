import { ApiScenario } from "../lib/api-scenario.js";

export const catalogScenarios = [
  new ApiScenario({
    key: "catalog.product.list",
    method: "GET",
    path: "/api/products",
  }),
  new ApiScenario({
    key: "catalog.product.search",
    method: "GET",
    path: "/api/products/search?keyword=k6",
  }),
  new ApiScenario({
    key: "catalog.product.detail",
    method: "GET",
    path: "/api/products/{productId}",
    seedProduct: true,
  }),
  new ApiScenario({
    key: "catalog.product.categories",
    method: "GET",
    path: "/api/products/categories",
  }),
  new ApiScenario({
    key: "catalog.product.posts",
    method: "GET",
    path: "/api/products/{productId}/posts",
    seedProduct: true,
  }),
];
