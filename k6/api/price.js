import { ApiScenario } from "../lib/api-scenario.js";

export const priceScenarios = [
  new ApiScenario({
    key: "price.history",
    method: "GET",
    path: "/api/products/{productId}/prices",
    seedProduct: true,
  }),
  new ApiScenario({
    key: "price.drops",
    method: "GET",
    path: "/api/products/price-drops",
  }),
];
