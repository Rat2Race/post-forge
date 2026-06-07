export function env(name, fallback) {
  const value = __ENV[name];
  return value === undefined || value === null || value === "" ? fallback : value;
}

export function envNumber(name, fallback) {
  const value = Number(__ENV[name]);
  return Number.isFinite(value) && value >= 0 ? value : fallback;
}

export function envBool(name, fallback) {
  const value = __ENV[name];
  if (value === undefined || value === null || value === "") return fallback;
  return String(value).toLowerCase() === "true";
}

export function positiveNumber(value) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : null;
}

export function parseJson(res) {
  try {
    return res.json();
  } catch {
    return null;
  }
}
