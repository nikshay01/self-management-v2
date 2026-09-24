const fs = require('fs');
const path = require('path');

const schemasPath = path.join(__dirname, 'schemas.js');
let schemasStr = fs.readFileSync(schemasPath, 'utf8');

// Temporarily convert to executable module
// Replace "const SCHEMAS =" with "module.exports ="
// Also remove trailing semicolon if exists
const execStr = schemasStr.replace('const SCHEMAS =', 'module.exports =');
const modulePath = path.join(__dirname, 'temp_schemas.js');
fs.writeFileSync(modulePath, execStr);

const SCHEMAS = require('./temp_schemas.js');

// 1. Add custom_metrics to every existing schema
for (const key in SCHEMAS) {
  if (SCHEMAS.hasOwnProperty(key)) {
    SCHEMAS[key].fields.custom_metrics = {
      type: "array", label: "Custom Metrics",
      itemFields: {
        metric_name: { type: "text", label: "Metric Name" },
        value: { type: "text", label: "Value" },
        intensity_or_scale: { type: "scale", min: 0, max: 10, label: "Intensity / Scale" },
        notes: { type: "textarea", label: "Notes" }
      }
    };
  }
}

// 2. Add habit_task_manager
SCHEMAS.habit_task_manager = {
  label: "Habit or Task Manager",
  fields: {
    name: { type: "text", required: true },
    description: { type: "textarea" },
    category: { type: "text" },
    subcategory: { type: "text" },
    project: { type: "text" },
    subproject: { type: "text" },
    priority: { type: "scale", min: 1, max: 5 },
    completionType: { type: "select", options: ["boolean", "duration", "percentage", "fraction", "count", "custom"] },
    targetValue: { type: "number" },
    targetUnit: { type: "text" },
    frequency_type: { type: "select", options: ["daily", "weekly", "monthly", "yearly", "custom"] },
    frequency_timesPerPeriod: { type: "number" },
    frequency_customDays: { type: "tags", placeholder: "e.g. monday, thursday" },
    timeBlock_enabled: { type: "boolean" },
    timeBlock_startTime: { type: "time" },
    timeBlock_endTime: { type: "time" },
    streak_current: { type: "number" },
    streak_longest: { type: "number" },
    streak_lastLoggedDate: { type: "date" },
    streak_allowedMissesBeforeBreak: { type: "number" },
    streak_currentConsecutiveMisses: { type: "number" },
    procrastination_enabled: { type: "boolean" },
    procrastination_totalMissedPeriods: { type: "number" },
    procrastination_missedDates: { type: "tags", placeholder: "YYYY-MM-DD format" },
    procrastination_longestMissStreak: { type: "number" },
    procrastination_currentMissStreak: { type: "number" },
    isActive: { type: "boolean" },
    createdAt: { type: "datetime" },
    updatedAt: { type: "datetime" },
    custom_metrics: {
      type: "array", label: "Custom Metrics",
      itemFields: {
        metric_name: { type: "text", label: "Metric Name" },
        value: { type: "text", label: "Value" },
        intensity_or_scale: { type: "scale", min: 0, max: 10, label: "Intensity / Scale" },
        notes: { type: "textarea", label: "Notes" }
      }
    }
  }
};

// 3. Add todo_system
SCHEMAS.todo_system = {
  label: "Todo System",
  fields: {
    title: { type: "text", required: true },
    description: { type: "textarea" },
    category: { type: "text" },
    subcategory: { type: "text" },
    project: { type: "text" },
    subproject: { type: "text" },
    priority: { type: "scale", min: 1, max: 5 },
    labels: { type: "tags" },
    deadline_type: { type: "select", options: ["exact", "within", "range", "anytime"] },
    deadline_exactDate: { type: "date" },
    deadline_windowStart: { type: "date" },
    deadline_windowEnd: { type: "date" },
    estimatedStartTime: { type: "datetime" },
    estimatedDuration: { type: "number", label: "Estimated Duration (minutes)" },
    estimatedEndTime: { type: "datetime" },
    actualStartTime: { type: "datetime" },
    actualEndTime: { type: "datetime" },
    actualDuration: { type: "number", label: "Actual Duration (minutes)" },
    status: { type: "select", options: ["todo", "in_progress", "completed", "procrastinated"] },
    completedAt: { type: "datetime" },
    procrastination_isActive: { type: "boolean" },
    procrastination_triggeredBy: { type: "select", options: ["auto", "manual"] },
    procrastination_clockStartedAt: { type: "datetime" },
    procrastination_totalMinutes: { type: "number" },
    procrastination_manuallyFlagged: { type: "boolean" },
    procrastination_checkpoints: {
      type: "array", label: "Procrastination Checkpoints",
      itemFields: {
        recordedAt: { type: "datetime" },
        minutesSoFar: { type: "number" }
      }
    },
    sessions: {
      type: "array", label: "Work Sessions",
      itemFields: {
        start: { type: "datetime" },
        end: { type: "datetime" },
        duration: { type: "number", label: "Duration (min)" }
      }
    },
    createdAt: { type: "datetime" },
    updatedAt: { type: "datetime" },
    custom_metrics: {
      type: "array", label: "Custom Metrics",
      itemFields: {
        metric_name: { type: "text", label: "Metric Name" },
        value: { type: "text", label: "Value" },
        intensity_or_scale: { type: "scale", min: 0, max: 10, label: "Intensity / Scale" },
        notes: { type: "textarea", label: "Notes" }
      }
    }
  }
};

// 4. Add daily_summary
SCHEMAS.daily_summary = {
  label: "Daily Summary",
  fields: {
    date: { type: "date", required: true },
    overall_day_rating: { type: "scale", min: 0, max: 10 },
    highlights: { type: "textarea", label: "Highlights of the day" },
    lowlights: { type: "textarea", label: "Lowlights / Challenges" },
    tasks_completed: { type: "number" },
    major_achievements: { type: "tags" },
    habits_completed_percent: { type: "number", label: "Habits Completion %" },
    mood_average: { type: "scale", min: 0, max: 10 },
    energy_average: { type: "scale", min: 0, max: 10 },
    stress_average: { type: "scale", min: 0, max: 10 },
    notes: { type: "textarea" },
    custom_metrics: {
      type: "array", label: "Custom Metrics",
      itemFields: {
        metric_name: { type: "text", label: "Metric Name" },
        value: { type: "text", label: "Value" },
        intensity_or_scale: { type: "scale", min: 0, max: 10, label: "Intensity / Scale" },
        notes: { type: "textarea", label: "Notes" }
      }
    }
  }
};

// Convert back to original format
const finalOutput = `// js/schemas.js\nconst SCHEMAS = ${JSON.stringify(SCHEMAS, null, 2)};\n`;

// Write back to schemas.js
fs.writeFileSync(schemasPath, finalOutput);
console.log('Schemas updated successfully.');

// Delete temp file
fs.unlinkSync(modulePath);
