import {ajax, showError, getDeviceUuid} from "./common.js";

const deviceUuid = getDeviceUuid();

const COLOR_PRESETS = [
    "#000000", "#404040", "#000080", "#5B0000", "#1B5E20",
    "#FFFFFF", "#F5F5DC", "#FFF8DC", "#E0E0E0", "#D7E8D0"
];

let fields = {};
let controls = {};
let enabledKeys = [];
let currentValues = null;

function eScope() {
    return document.getElementById("scope");
}

function collectSettings() {
    document.querySelectorAll("#settings-form [data-setting]").forEach(field => {
        const key = field.getAttribute("data-setting");
        const control = field.querySelector("input, select");
        fields[key] = field;
        controls[key] = control;
        control.addEventListener("change", saveSettings);
    });
}

function loadDefinitions() {
    ajax(`/api/v1/settings/definitions?scope=${eScope().value}`)
        .then(keys => {
            enabledKeys = keys;
            applyVisibility();
            loadSettings();
        })
        .catch(error => showError(error.message));
}

function applyVisibility() {
    for (const [key, field] of Object.entries(fields)) {
        field.hidden = !enabledKeys.includes(key);
    }
    const paddingKeys = ["paddingLeft", "paddingTop", "paddingRight", "paddingBottom"];
    const group = document.getElementById("padding-group");
    if (group) {
        group.hidden = !paddingKeys.some(key => enabledKeys.includes(key));
    }
}

function normalizeColor(value) {
    const match = value.match(/^#([0-9a-f])([0-9a-f])([0-9a-f])$/i);
    if (match) {
        return "#" + [match[1], match[2], match[3]].map(c => c + c).join("");
    }
    return value;
}

function applyValues(values) {
    for (const [key, control] of Object.entries(controls)) {
        if (values[key] === undefined) {
            continue;
        }
        if (control.type === "checkbox") {
            control.checked = values[key] === "true";
        } else {
            control.value = control.type === "color" ? normalizeColor(values[key]) : values[key];
        }
    }
    updatePreview();
}

function updatePreview() {
    const preview = document.getElementById("padding-preview");
    const color = controls["color"];
    const background = controls["background"];
    if (!preview || !color || !background) {
        return;
    }
    preview.style.backgroundColor = background.value;
    preview.style.color = color.value;
    const font = controls["font"];
    if (font && font.value) {
        preview.style.fontFamily = font.value;
    }
}

function loadSettings() {
    ajax(`/api/v1/settings?deviceUuid=${deviceUuid}&scope=${eScope().value}`)
        .then(settings => {
            currentValues = Object.fromEntries(settings.settings.map(kv => [kv.key, kv.value]));
            applyValues(currentValues);
        })
        .catch(error => showError(error.message));
}

function saveSettings() {
    updatePreview();
    const values = enabledKeys
        .filter(key => controls[key])
        .map(key => ({key: key, value: valueOf(controls[key])}));
    ajax(`/api/v1/settings?deviceUuid=${deviceUuid}&scope=${eScope().value}`, {
        method: "PUT",
        body: values,
        json: false
    }).catch(error => showError(error.message));
}

function valueOf(control) {
    return control.type === "checkbox" ? String(control.checked) : control.value;
}

function initColorPresets() {
    document.querySelectorAll("#settings-form .color-presets").forEach(row => {
        const input = row.parentElement.querySelector("input[type=color]");
        COLOR_PRESETS.forEach(color => {
            const swatch = document.createElement("button");
            swatch.type = "button";
            swatch.className = "color-swatch";
            swatch.style.backgroundColor = color;
            swatch.title = color;
            swatch.addEventListener("click", () => {
                input.value = color;
                saveSettings();
            });
            row.appendChild(swatch);
        });
    });
}

function loadFonts() {
    const select = controls["font"];
    if (!select) {
        return;
    }
    ajax("/api/v1/settings/fonts")
        .then(data => {
            select.innerHTML = "";
            data.forEach(f => {
                let option = document.createElement("option");
                option.value = f;
                option.textContent = f;
                select.appendChild(option);
            });
            if (currentValues) {
                applyValues(currentValues);
            }
        })
        .catch(error => showError(error.message));
}

function applyRequestedScope() {
    const requested = new URLSearchParams(window.location.search).get("scope");
    if (requested && Array.from(eScope().options).some(o => o.value === requested)) {
        eScope().value = requested;
    }
}

function goBack() {
    if (window.history.length > 1) {
        window.history.back();
    } else {
        window.location.href = "/";
    }
}

function init() {
    eScope().addEventListener("change", loadDefinitions);
    document.getElementById("back").addEventListener("click", goBack);
    applyRequestedScope();
    collectSettings();
    initColorPresets();
    loadFonts();
    loadDefinitions();
}

document.addEventListener("DOMContentLoaded", init);
