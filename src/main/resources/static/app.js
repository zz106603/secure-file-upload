const FIXED_ORDER = ["bat", "cmd", "com", "cpl", "exe", "scr", "js"];
const fixedContainer = document.querySelector("#fixed-extensions");
const fixedMessage = document.querySelector("#fixed-message");
const customContainer = document.querySelector("#custom-extensions");
const customCount = document.querySelector("#custom-count");
const customForm = document.querySelector("#custom-form");
const customInput = document.querySelector("#custom-extension");
const addCustomButton = document.querySelector("#add-custom-button");
const customMessage = document.querySelector("#custom-message");
const uploadForm = document.querySelector("#upload-form");
const uploadFile = document.querySelector("#upload-file");
const uploadButton = document.querySelector("#upload-button");
const uploadMessage = document.querySelector("#upload-message");

let fixedPolicies = [];
let customPolicies = [];

class RequestError extends Error {
  constructor(message, type) {
    super(message);
    this.type = type;
  }
}

async function request(url, options = {}) {
  let response;
  try {
    response = await fetch(url, options);
  } catch {
    throw new RequestError("서버와 통신할 수 없습니다. 다시 시도해 주세요.", "network");
  }

  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    throw new RequestError(body?.message || "요청을 처리하지 못했습니다. 다시 시도해 주세요.", "server");
  }
  return body;
}

function showMessage(element, message = "", type = "") {
  element.textContent = message;
  element.className = `message ${type}`;
}

function showRequestError(element, error) {
  showMessage(element, error.message, "error");
}

function renderFixedPolicies() {
  fixedContainer.replaceChildren();
  const policiesByExtension = new Map(fixedPolicies.map((policy) => [policy.extension, policy]));
  FIXED_ORDER.forEach((extension) => {
    const policy = policiesByExtension.get(extension);
    if (!policy) return;
    const label = document.createElement("label");
    label.className = "check-item";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = policy.blocked;
    checkbox.addEventListener("change", () => changeFixedPolicy(policy, checkbox));
    const text = document.createElement("span");
    text.textContent = extension;
    label.append(checkbox, text);
    fixedContainer.append(label);
  });
}

async function changeFixedPolicy(policy, checkbox) {
  const requestedBlocked = checkbox.checked;
  // 서버 저장 전에는 기존 체크 상태를 유지해 실패 시 화면과 DB가 어긋나지 않게 한다.
  checkbox.checked = policy.blocked;
  checkbox.disabled = true;
  try {
    const saved = await request(`/api/extension-policies/fixed/${policy.id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ blocked: requestedBlocked })
    });
    fixedPolicies = fixedPolicies.map((item) => item.id === saved.id ? saved : item);
  } catch (error) {
    showRequestError(fixedMessage, error);
  } finally {
    renderFixedPolicies();
  }
}

function renderCustomPolicies() {
  customContainer.replaceChildren();
  customCount.textContent = `${customPolicies.length}/200`;
  customPolicies.forEach((policy) => {
    const tag = document.createElement("div");
    tag.className = "tag";
    const extension = document.createElement("span");
    extension.textContent = policy.extension;
    const remove = document.createElement("button");
    remove.className = "tag-delete";
    remove.type = "button";
    remove.textContent = "×";
    remove.setAttribute("aria-label", `${policy.extension} 확장자 삭제`);
    remove.addEventListener("click", () => deleteCustomPolicy(policy, remove));
    tag.append(extension, remove);
    customContainer.append(tag);
  });
}

async function loadPolicies() {
  try {
    const [fixed, custom] = await Promise.all([
      request("/api/extension-policies/fixed"),
      request("/api/extension-policies/custom")
    ]);
    fixedPolicies = fixed;
    customPolicies = custom;
    renderFixedPolicies();
    renderCustomPolicies();
  } catch (error) {
    showRequestError(customMessage, error);
  }
}

customForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  addCustomButton.disabled = true;
  customInput.disabled = true;
  showMessage(customMessage);
  try {
    const saved = await request("/api/extension-policies/custom", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ extension: customInput.value })
    });
    // 서버가 저장에 성공한 응답을 받은 뒤에만 목록과 개수를 바꾼다.
    customInput.value = "";
    customPolicies = [...customPolicies, saved].sort((left, right) => left.extension.localeCompare(right.extension));
    renderCustomPolicies();
    showMessage(customMessage, "커스텀 확장자를 추가했습니다.", "success");
  } catch (error) {
    showRequestError(customMessage, error);
  } finally {
    addCustomButton.disabled = false;
    customInput.disabled = false;
  }
});

async function deleteCustomPolicy(policy, button) {
  button.disabled = true;
  showMessage(customMessage);
  try {
    await request(`/api/extension-policies/custom/${policy.id}`, { method: "DELETE" });
    customPolicies = customPolicies.filter((item) => item.id !== policy.id);
    renderCustomPolicies();
    showMessage(customMessage, "커스텀 확장자를 삭제했습니다.", "success");
  } catch (error) {
    button.disabled = false;
    showRequestError(customMessage, error);
  }
}

uploadForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!uploadFile.files.length) {
    showMessage(uploadMessage, "업로드할 파일을 선택해 주세요.", "error");
    return;
  }
  uploadButton.disabled = true;
  uploadFile.disabled = true;
  showMessage(uploadMessage);
  try {
    const formData = new FormData();
    formData.append("file", uploadFile.files[0]);
    await request("/api/files", { method: "POST", body: formData });
    uploadForm.reset();
    showMessage(uploadMessage, "파일 업로드가 완료되었습니다.", "success");
  } catch (error) {
    showRequestError(uploadMessage, error);
  } finally {
    uploadButton.disabled = false;
    uploadFile.disabled = false;
  }
});

loadPolicies();
