import "../styles.css";

import confetti from "canvas-confetti";
import $ from "jquery";
import DataTable from "datatables.net";
import ApexCharts from "apexcharts";
import _ from "lodash";
import ClipboardJS from "clipboard";
import flatpickr from "flatpickr";

import { initClipboardButtons } from "./modules/clipboard-buttons.js";

const dataTableApi = DataTable?.default ?? DataTable;
const clipboardFactory = ClipboardJS?.default ?? ClipboardJS;
const flatpickrFactory = flatpickr?.default ?? flatpickr;

window.$ = $;
window.jQuery = $;
window.ApexCharts = ApexCharts;
window.buildChart = (selector, optionsFactory) => {
  const target = document.querySelector(selector);
  if (!target || typeof optionsFactory !== "function") {
    return null;
  }

  const chart = new ApexCharts(target, optionsFactory());
  chart.render();
  return chart;
};
window._ = _;
window.ClipboardJS = clipboardFactory;
window.flatpickr = flatpickrFactory;
window.confetti = confetti;
window.DataTable = dataTableApi;

const initFlyonUi = async () => {
  await import("flyonui/flyonui.js");
  window.HSStaticMethods?.autoInit();
};

void initFlyonUi();

document.addEventListener("DOMContentLoaded", () => {
  initClipboardButtons();
});
