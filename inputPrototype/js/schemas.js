// js/schemas.js
const SCHEMAS = {
  "body_metric": {
    "label": "Body Metric",
    "fields": {
      "recorded_at": {
        "type": "datetime",
        "required": true
      },
      "measurements": {
        "type": "group",
        "label": "Measurements",
        "fields": {
          "arms": {
            "type": "group",
            "label": "Arms",
            "fields": {
              "left_bicep_cm": {
                "type": "number"
              },
              "right_bicep_cm": {
                "type": "number"
              },
              "left_forearm_cm": {
                "type": "number"
              },
              "right_forearm_cm": {
                "type": "number"
              },
              "left_upper_arm_cm": {
                "type": "number"
              },
              "right_upper_arm_cm": {
                "type": "number"
              }
            }
          },
          "torso": {
            "type": "group",
            "label": "Torso",
            "fields": {
              "chest_relaxed_cm": {
                "type": "number"
              },
              "chest_flexed_cm": {
                "type": "number"
              },
              "waist_cm": {
                "type": "number"
              },
              "abdominal_cm": {
                "type": "number"
              },
              "hips_cm": {
                "type": "number"
              }
            }
          },
          "shoulders": {
            "type": "group",
            "label": "Shoulders",
            "fields": {
              "left_cm": {
                "type": "number"
              },
              "right_cm": {
                "type": "number"
              },
              "width_cm": {
                "type": "number"
              }
            }
          },
          "legs": {
            "type": "group",
            "label": "Legs",
            "fields": {
              "left_thigh_cm": {
                "type": "number"
              },
              "right_thigh_cm": {
                "type": "number"
              },
              "left_calf_cm": {
                "type": "number"
              },
              "right_calf_cm": {
                "type": "number"
              }
            }
          },
          "body": {
            "type": "group",
            "label": "Body",
            "fields": {
              "height_cm": {
                "type": "number"
              },
              "weight_kg": {
                "type": "number"
              },
              "neck_cm": {
                "type": "number"
              },
              "wrist_cm": {
                "type": "number"
              },
              "body_fat_percent": {
                "type": "number"
              }
            }
          }
        }
      },
      "notes": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "devotion": {
    "label": "Devotion Session",
    "fields": {
      "timestamp_start": {
        "type": "datetime",
        "required": true
      },
      "timestamp_end": {
        "type": "datetime"
      },
      "duration_minutes": {
        "type": "number"
      },
      "time_category": {
        "type": "select",
        "options": [
          "brahma-muhurta",
          "morning",
          "noon",
          "evening",
          "night"
        ]
      },
      "devotion_type": {
        "type": "select",
        "options": [
          "mandir",
          "puja",
          "aarti",
          "jaap",
          "path",
          "kirtan",
          "seva",
          "satsang",
          "vrat",
          "other"
        ]
      },
      "deity_focus": {
        "type": "select",
        "options": [
          "Shiva",
          "Vishnu",
          "Devi",
          "Ganesha",
          "Ram",
          "Krishna",
          "other"
        ]
      },
      "location": {
        "type": "select",
        "options": [
          "home-mandir",
          "temple",
          "ashram",
          "river",
          "open-nature",
          "other"
        ]
      },
      "temple_name": {
        "type": "text"
      },
      "guided": {
        "type": "boolean"
      },
      "guide_type": {
        "type": "select",
        "options": [
          "pandit",
          "guru",
          "organization",
          "self"
        ]
      },
      "guide_name": {
        "type": "text"
      },
      "organization_name": {
        "type": "text"
      },
      "posture": {
        "type": "select",
        "options": [
          "standing",
          "seated",
          "kneeling",
          "prostrating"
        ]
      },
      "pre_mood": {
        "type": "select",
        "options": [
          "anxious",
          "sad",
          "neutral",
          "calm",
          "devotional",
          "joyful"
        ]
      },
      "post_mood": {
        "type": "select",
        "options": [
          "anxious",
          "sad",
          "neutral",
          "calm",
          "devotional",
          "joyful"
        ]
      },
      "pre_stress_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_stress_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "stress_delta": {
        "type": "computed",
        "formula": "pre_stress_level - post_stress_level",
        "label": "Stress Delta (auto)"
      },
      "emotional_intensity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sense_of_peace": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "focus_during": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "gratitude_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "bhakti_felt": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "surrender_felt": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "divine_connection_felt": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "custom_fields": {
        "type": "array",
        "label": "Custom fields (e.g. ram name chants)",
        "itemFields": {
          "field_name": {
            "type": "text",
            "label": "Field Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          }
        }
      },
      "context_note": {
        "type": "textarea"
      },
      "pre_activity": {
        "type": "select",
        "options": [
          "sleep",
          "exercise",
          "work",
          "eating",
          "commute",
          "other"
        ]
      },
      "context_location": {
        "type": "select",
        "options": [
          "home",
          "work",
          "travel",
          "pilgrimage",
          "other"
        ]
      },
      "social_context": {
        "type": "select",
        "options": [
          "alone",
          "with family",
          "with community",
          "with guru"
        ]
      },
      "session_rating": {
        "type": "scale",
        "min": 0,
        "max": 5
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "gaming_session": {
    "label": "Gaming Session",
    "fields": {
      "timestamp_start": {
        "type": "datetime",
        "required": true
      },
      "timestamp_end": {
        "type": "datetime"
      },
      "duration_minutes": {
        "type": "number"
      },
      "game_name": {
        "type": "text",
        "required": true
      },
      "platform": {
        "type": "select",
        "options": [
          "PC",
          "Console",
          "Mobile",
          "VR",
          "Tabletop"
        ]
      },
      "session_type": {
        "type": "select",
        "options": [
          "casual",
          "competitive",
          "story/campaign",
          "grinding",
          "social/co-op",
          "esports/tournament"
        ]
      },
      "social_context": {
        "type": "select",
        "options": [
          "solo",
          "friends online",
          "local co-op",
          "randoms"
        ]
      },
      "enjoyment": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "performance_rating": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "tilt_frustration_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "flow_state_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "fatigue_after": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "eye_strain": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "posture_quality": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "matches": {
        "type": "group",
        "label": "Matches (Optional)",
        "fields": {
          "played": {
            "type": "number"
          },
          "won": {
            "type": "number"
          },
          "lost": {
            "type": "number"
          }
        }
      },
      "notes": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "health_log": {
    "label": "Health Log",
    "fields": {
      "date": {
        "type": "date",
        "required": true
      },
      "morning_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "midday_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "evening_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "morning_body_stiffness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "soreness_scale": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "headache_intensity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "digestion_quality": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "hydration_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sick_status": {
        "type": "boolean"
      },
      "sickness_intensity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "symptoms": {
        "type": "tags",
        "placeholder": "e.g. fever, headache, nausea"
      },
      "breathing_quality": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "cough_intensity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "mental_fatigue": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "physical_fatigue": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "stomach_discomfort": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "resting_heart_rate": {
        "type": "number"
      },
      "context_note": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "hobbies": {
    "label": "Hobby (New Entry)",
    "fields": {
      "name": {
        "type": "text",
        "required": true
      },
      "category": {
        "type": "text"
      },
      "subcategory": {
        "type": "text"
      },
      "description": {
        "type": "textarea"
      },
      "intensity_type": {
        "type": "select",
        "options": [
          "low",
          "moderate",
          "high",
          "extreme"
        ]
      },
      "dopamine_type": {
        "type": "select",
        "options": [
          "instant",
          "delayed",
          "flow-state",
          "social"
        ]
      },
      "skill_growth_possible": {
        "type": "boolean"
      },
      "consistency_goal": {
        "type": "select",
        "options": [
          "daily",
          "weekly",
          "monthly",
          "occasional"
        ]
      },
      "current_level": {
        "type": "text"
      },
      "self_rated_skill": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "target_level": {
        "type": "text"
      },
      "milestones": {
        "type": "array",
        "label": "Milestones",
        "itemFields": {
          "title": {
            "type": "text"
          },
          "achieved_at": {
            "type": "date"
          },
          "note": {
            "type": "textarea"
          }
        }
      },
      "goals": {
        "type": "array",
        "label": "Goals",
        "itemFields": {
          "title": {
            "type": "text"
          },
          "type": {
            "type": "select",
            "options": [
              "time",
              "count",
              "skill",
              "streak",
              "custom"
            ]
          },
          "target_value": {
            "type": "number"
          },
          "target_unit": {
            "type": "text"
          },
          "target_period": {
            "type": "select",
            "options": [
              "daily",
              "weekly",
              "monthly",
              "yearly",
              "lifetime"
            ]
          },
          "is_active": {
            "type": "boolean"
          }
        }
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "hobby_session": {
    "label": "Hobby Session",
    "fields": {
      "hobby_id": {
        "type": "text",
        "label": "Hobby ID / Name",
        "required": true
      },
      "started_at": {
        "type": "datetime",
        "required": true
      },
      "ended_at": {
        "type": "datetime"
      },
      "duration_minutes": {
        "type": "number"
      },
      "location": {
        "type": "text"
      },
      "main_focus": {
        "type": "text",
        "label": "Main Focus of Session"
      },
      "immersion_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "notes": {
        "type": "textarea"
      },
      "attachment_text": {
        "type": "textarea",
        "label": "Attachment / Notes (text)"
      },
      "overall_session_quality": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "focus": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "enjoyment": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "progress": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "difficulty": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "mood_snapshot": {
        "type": "group",
        "label": "Mood Snapshot (Optional)",
        "fields": {
          "mood": {
            "type": "scale",
            "min": 0,
            "max": 10
          },
          "energy": {
            "type": "scale",
            "min": 0,
            "max": 10
          },
          "stress": {
            "type": "scale",
            "min": 0,
            "max": 10
          }
        }
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "meditation": {
    "label": "Meditation Session",
    "fields": {
      "timestamp_start": {
        "type": "datetime",
        "required": true
      },
      "timestamp_end": {
        "type": "datetime"
      },
      "duration_minutes": {
        "type": "number"
      },
      "type": {
        "type": "select",
        "options": [
          "breath",
          "mantra",
          "guided",
          "silent",
          "walking",
          "body_scan",
          "mindfulness",
          "spiritual",
          "focused",
          "movement",
          "transcendental",
          "progressive_relaxation",
          "loving_kindness",
          "visualization"
        ]
      },
      "technique": {
        "type": "select",
        "options": [
          "box_breathing",
          "raam_nam",
          "mindfulness",
          "visualization",
          "other"
        ]
      },
      "posture": {
        "type": "select",
        "options": [
          "sitting",
          "lying",
          "walking",
          "kneeling"
        ]
      },
      "environment": {
        "type": "select",
        "options": [
          "quiet",
          "music",
          "nature",
          "noisy"
        ]
      },
      "back_support_used": {
        "type": "boolean"
      },
      "music_used": {
        "type": "text"
      },
      "pre_stress_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_energy_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_mood": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_mental_clarity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_brain_fog": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_tiredness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "depth_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "distraction_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "focus_percentage": {
        "type": "number"
      },
      "distraction_spikes_count": {
        "type": "number"
      },
      "sleepiness_peak": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "mantra_count": {
        "type": "number"
      },
      "body_relaxation_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "heart_rate_before": {
        "type": "number"
      },
      "heart_rate_after": {
        "type": "number"
      },
      "post_stress_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "mental_clarity_after": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "calmness_after": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "energy_after": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "brain_fog_after": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "motivation_after": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sense_of_presence": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "devotional_feeling": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "gratitude_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "effective_focus_minutes": {
        "type": "number"
      },
      "context_note": {
        "type": "textarea"
      },
      "location": {
        "type": "text"
      },
      "background_audio": {
        "type": "text"
      },
      "session_intention": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "mood_entry": {
    "label": "Mood Entry",
    "fields": {
      "timestamp": {
        "type": "datetime",
        "required": true
      },
      "mood": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "mental_clarity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "calmness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "anxiety": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "tiredness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sleepiness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "confidence": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "motivation": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "frustration": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "stress": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "overthinking": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "brain_fog": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sickness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "laziness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "boredom": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "mindfulness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "emotions": {
        "type": "array",
        "label": "Emotions (multiple)",
        "itemFields": {
          "emotion": {
            "type": "text"
          },
          "intensity": {
            "type": "scale",
            "min": 0,
            "max": 10
          }
        }
      },
      "context_activity": {
        "type": "text"
      },
      "context_location": {
        "type": "text"
      },
      "context_social": {
        "type": "select",
        "options": [
          "alone",
          "with friends",
          "with family",
          "with partner",
          "in public",
          "online"
        ]
      },
      "context_note": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "nutrition": {
    "label": "Nutrition Log",
    "fields": {
      "date": {
        "type": "date",
        "required": true
      },
      "meals": {
        "type": "array",
        "label": "Meals",
        "itemFields": {
          "name": {
            "type": "select",
            "options": [
              "breakfast",
              "lunch",
              "snack",
              "dinner",
              "pre-workout",
              "post-workout",
              "other"
            ]
          },
          "timestamp": {
            "type": "datetime"
          },
          "items": {
            "type": "array",
            "label": "Food Items",
            "itemFields": {
              "food_name": {
                "type": "text"
              },
              "quantity": {
                "type": "number"
              },
              "unit": {
                "type": "select",
                "options": [
                  "g",
                  "ml",
                  "piece",
                  "bowl",
                  "tsp",
                  "tbsp",
                  "cup"
                ]
              },
              "calories_kcal": {
                "type": "number"
              },
              "protein_g": {
                "type": "number"
              },
              "carbs_g": {
                "type": "number"
              },
              "fat_g": {
                "type": "number"
              },
              "fiber_g": {
                "type": "number"
              }
            }
          }
        }
      },
      "daily_calories_kcal": {
        "type": "number",
        "label": "Daily Total Calories (kcal)"
      },
      "daily_protein_g": {
        "type": "number",
        "label": "Daily Total Protein (g)"
      },
      "daily_carbs_g": {
        "type": "number",
        "label": "Daily Total Carbs (g)"
      },
      "daily_fat_g": {
        "type": "number",
        "label": "Daily Total Fat (g)"
      },
      "daily_fiber_g": {
        "type": "number",
        "label": "Daily Total Fiber (g)"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "pain_log": {
    "label": "Pain Log",
    "fields": {
      "timestamp": {
        "type": "datetime",
        "required": true
      },
      "body_parts": {
        "type": "tags",
        "label": "Body Parts / Muscles",
        "placeholder": "e.g. left knee, lower back"
      },
      "pain_type": {
        "type": "select",
        "options": [
          "sharp",
          "dull",
          "throbbing",
          "burning",
          "aching",
          "stabbing",
          "cramping",
          "pressure",
          "other"
        ]
      },
      "intensity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pain_start": {
        "type": "datetime",
        "label": "Pain Started At"
      },
      "pain_end": {
        "type": "datetime",
        "label": "Pain Ended At"
      },
      "duration_minutes": {
        "type": "number"
      },
      "possible_trigger": {
        "type": "text"
      },
      "context_note": {
        "type": "textarea"
      },
      "post_pain_avg_intensity": {
        "type": "number",
        "label": "Post-Log Avg Intensity"
      },
      "post_pain_total_duration_minutes": {
        "type": "number",
        "label": "Post-Log Total Duration (mins)"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "scores": {
    "label": "Daily Scores",
    "fields": {
      "date": {
        "type": "date",
        "required": true
      },
      "daily_wellness_score": {
        "type": "number",
        "label": "Daily Wellness Score (0-100)"
      },
      "productivity_index": {
        "type": "number",
        "label": "Productivity Index (0-100)"
      },
      "spiritual_score": {
        "type": "number",
        "label": "Spiritual Score (0-100)"
      },
      "mental_health_score": {
        "type": "number",
        "label": "Mental Health Score (0-100)"
      },
      "physical_health_score": {
        "type": "number",
        "label": "Physical Health Score (0-100)"
      },
      "sleep_score": {
        "type": "number",
        "label": "Sleep Score (0-100)"
      },
      "screen_discipline_score": {
        "type": "number",
        "label": "Screen Discipline Score (0-100)"
      },
      "hobby_growth_score": {
        "type": "number",
        "label": "Hobby Growth Score (0-100)"
      },
      "self_control_score": {
        "type": "number",
        "label": "Self Control Score (0-100)"
      },
      "day_score": {
        "type": "number",
        "label": "Master Day Score (0-100)"
      },
      "day_rating": {
        "type": "select",
        "options": [
          "Elite",
          "Great",
          "Good",
          "Average",
          "Rough",
          "Bad"
        ]
      },
      "wellness_breakdown": {
        "type": "group",
        "label": "Wellness Breakdown",
        "fields": {
          "energy_avg": {
            "type": "number"
          },
          "sleep_quality": {
            "type": "number"
          },
          "mood_avg": {
            "type": "number"
          },
          "negative_symptoms_inv": {
            "type": "number"
          },
          "physical_state_inv": {
            "type": "number"
          },
          "hydration_digestion": {
            "type": "number"
          },
          "mindfulness": {
            "type": "number"
          }
        }
      },
      "productivity_breakdown": {
        "type": "group",
        "label": "Productivity Breakdown",
        "fields": {
          "focus": {
            "type": "number"
          },
          "output_quality": {
            "type": "number"
          },
          "goal_adherence": {
            "type": "number"
          },
          "efficiency": {
            "type": "number"
          },
          "mental_state": {
            "type": "number"
          },
          "distraction_control": {
            "type": "number"
          }
        }
      },
      "spiritual_breakdown": {
        "type": "group",
        "label": "Spiritual Breakdown",
        "fields": {
          "meditation_depth": {
            "type": "number"
          },
          "devotion_intensity": {
            "type": "number"
          },
          "inner_peace": {
            "type": "number"
          },
          "stress_reduction": {
            "type": "number"
          },
          "consistency_bonus": {
            "type": "number"
          }
        }
      },
      "mental_health_breakdown": {
        "type": "group",
        "label": "Mental Health Breakdown",
        "fields": {
          "mood_calm": {
            "type": "number"
          },
          "clarity_focus": {
            "type": "number"
          },
          "emotional_balance": {
            "type": "number"
          },
          "drive": {
            "type": "number"
          },
          "fatigue_inv": {
            "type": "number"
          }
        }
      },
      "physical_health_breakdown": {
        "type": "group",
        "label": "Physical Health Breakdown",
        "fields": {
          "energy": {
            "type": "number"
          },
          "pain_inv": {
            "type": "number"
          },
          "recovery": {
            "type": "number"
          },
          "digestion": {
            "type": "number"
          },
          "respiratory": {
            "type": "number"
          },
          "hydration": {
            "type": "number"
          },
          "illness_inv": {
            "type": "number"
          }
        }
      },
      "sleep_breakdown": {
        "type": "group",
        "label": "Sleep Breakdown",
        "fields": {
          "quality": {
            "type": "number"
          },
          "duration_accuracy": {
            "type": "number"
          },
          "schedule_adherence": {
            "type": "number"
          },
          "interruptions_inv": {
            "type": "number"
          },
          "sleep_debt_inv": {
            "type": "number"
          }
        }
      },
      "screen_discipline_breakdown": {
        "type": "group",
        "label": "Screen Discipline Breakdown",
        "fields": {
          "intentional_use": {
            "type": "number"
          },
          "productive_ratio": {
            "type": "number"
          },
          "time_control": {
            "type": "number"
          },
          "short_form_avoidance": {
            "type": "number"
          },
          "late_night_avoidance": {
            "type": "number"
          },
          "unlock_discipline": {
            "type": "number"
          }
        }
      },
      "hobby_growth_breakdown": {
        "type": "group",
        "label": "Hobby Growth Breakdown",
        "fields": {
          "session_quality": {
            "type": "number"
          },
          "enjoyment": {
            "type": "number"
          },
          "immersion": {
            "type": "number"
          },
          "progress": {
            "type": "number"
          },
          "focus": {
            "type": "number"
          },
          "streak_bonus": {
            "type": "number"
          }
        }
      },
      "self_control_breakdown": {
        "type": "group",
        "label": "Self Control Breakdown",
        "fields": {
          "screen_discipline": {
            "type": "number"
          },
          "work_distraction_control": {
            "type": "number"
          },
          "urge_resistance": {
            "type": "number"
          },
          "meditation_focus": {
            "type": "number"
          },
          "regret_avoidance": {
            "type": "number"
          },
          "schedule_adherence": {
            "type": "number"
          }
        }
      },
      "trends": {
        "type": "group",
        "label": "Trends",
        "fields": {
          "meditation_progress": {
            "type": "number"
          },
          "mood_trend": {
            "type": "number"
          },
          "sleep_consistency": {
            "type": "number"
          },
          "body_composition_trend": {
            "type": "number"
          },
          "habit_streak_score": {
            "type": "number"
          },
          "pain_trend": {
            "type": "number"
          },
          "nutrition_score": {
            "type": "number"
          },
          "sexual_health_trend": {
            "type": "number"
          }
        }
      },
      "computed_at": {
        "type": "datetime"
      },
      "data_completeness_percent": {
        "type": "number"
      },
      "missing_sources": {
        "type": "tags",
        "placeholder": "e.g. nutrition, screentime"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "screentime": {
    "label": "Screen Time",
    "fields": {
      "date": {
        "type": "date",
        "required": true
      },
      "total_screen_time_minutes": {
        "type": "number"
      },
      "unlock_count": {
        "type": "number"
      },
      "social_media_minutes": {
        "type": "number"
      },
      "entertainment_minutes": {
        "type": "number"
      },
      "productive_minutes": {
        "type": "number"
      },
      "learning_minutes": {
        "type": "number"
      },
      "gaming_minutes": {
        "type": "number"
      },
      "short_form_used": {
        "type": "boolean"
      },
      "late_night_usage": {
        "type": "boolean"
      },
      "in_bed_usage": {
        "type": "boolean"
      },
      "intentional_percent": {
        "type": "number"
      },
      "autopilot_percent": {
        "type": "computed",
        "formula": "100 - intentional_percent",
        "label": "Autopilot % (auto)"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "sexual_session": {
    "label": "Sexual Session",
    "fields": {
      "timestamp_start": {
        "type": "datetime",
        "required": true
      },
      "timestamp_end": {
        "type": "datetime"
      },
      "time_of_day": {
        "type": "select",
        "options": [
          "early-morning",
          "morning",
          "afternoon",
          "evening",
          "night",
          "late-night"
        ]
      },
      "session_type": {
        "type": "select",
        "options": [
          "masturbation",
          "partnered sex",
          "mutual masturbation",
          "oral",
          "other"
        ]
      },
      "session_goal": {
        "type": "text"
      },
      "duration_minutes": {
        "type": "number"
      },
      "gap_since_last_session_hours": {
        "type": "number"
      },
      "trigger_type": {
        "type": "select",
        "options": [
          "habit",
          "boredom",
          "sexual desire",
          "partner interaction",
          "stress relief",
          "other"
        ]
      },
      "medium_type": {
        "type": "select",
        "options": [
          "none",
          "visual static",
          "visual dynamic",
          "interactive",
          "digital interactive"
        ]
      },
      "porn_used": {
        "type": "boolean"
      },
      "arousal_intensity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "conscious_control": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "erection_state": {
        "type": "select",
        "options": [
          "full",
          "partial",
          "weak",
          "none",
          "N/A"
        ]
      },
      "edging_occurred": {
        "type": "boolean"
      },
      "edging_duration_minutes": {
        "type": "number"
      },
      "release_occurred": {
        "type": "boolean"
      },
      "toy_used": {
        "type": "boolean"
      },
      "lubricant_used": {
        "type": "boolean"
      },
      "pain": {
        "type": "boolean"
      },
      "pain_points": {
        "type": "text"
      },
      "urge_resistance_attempts": {
        "type": "number"
      },
      "pre_mood": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_stress": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_anxiety": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_guilt": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_brain_fog": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_mental_clarity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_mood": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_stress": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_anxiety": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_guilt": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_brain_fog": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_mental_clarity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "orgasm_quality": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "regret_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "context_note": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "sleep": {
    "label": "Sleep",
    "fields": {
      "date": {
        "type": "date",
        "required": true
      },
      "sleep_start": {
        "type": "datetime"
      },
      "sleep_end": {
        "type": "datetime"
      },
      "total_sleep_hours": {
        "type": "number"
      },
      "target_sleep_time": {
        "type": "time"
      },
      "target_wake_time": {
        "type": "time"
      },
      "target_sleep_hours": {
        "type": "number"
      },
      "sleep_start_delta_minutes": {
        "type": "number"
      },
      "wake_time_delta_minutes": {
        "type": "number"
      },
      "sleep_hours_delta": {
        "type": "number"
      },
      "sleep_quality": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "restfulness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sleepiness_on_wake": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "ease_of_falling_asleep": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sleep_interruptions_count": {
        "type": "number"
      },
      "interruptions": {
        "type": "array",
        "label": "Interruptions",
        "itemFields": {
          "wake_time": {
            "type": "time"
          },
          "back_to_sleep_time": {
            "type": "time"
          },
          "duration_minutes": {
            "type": "number"
          },
          "reason": {
            "type": "text"
          }
        }
      },
      "total_interruption_minutes": {
        "type": "number"
      },
      "sleep_debt_hours": {
        "type": "number"
      },
      "naps": {
        "type": "array",
        "label": "Naps",
        "itemFields": {
          "start_time": {
            "type": "time"
          },
          "end_time": {
            "type": "time"
          },
          "duration_minutes": {
            "type": "number"
          },
          "quality": {
            "type": "scale",
            "min": 0,
            "max": 10
          }
        }
      },
      "total_nap_minutes": {
        "type": "number"
      },
      "dream_count": {
        "type": "number"
      },
      "dreams": {
        "type": "array",
        "label": "Dreams",
        "itemFields": {
          "description": {
            "type": "textarea"
          },
          "vividness": {
            "type": "scale",
            "min": 0,
            "max": 10
          },
          "emotional_tone": {
            "type": "select",
            "options": [
              "positive",
              "negative",
              "neutral",
              "mixed"
            ]
          },
          "lucid": {
            "type": "boolean"
          }
        }
      },
      "pre_stress": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_anxiety": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "pre_screen_time_minutes": {
        "type": "number"
      },
      "caffeine_intake_after_2pm": {
        "type": "boolean"
      },
      "late_meal": {
        "type": "boolean"
      },
      "pre_bed_shower": {
        "type": "boolean"
      },
      "pre_activity": {
        "type": "select",
        "options": [
          "exercise",
          "work",
          "screen",
          "reading",
          "meditation",
          "other"
        ]
      },
      "post_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_mood": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_mental_clarity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "post_body_stiffness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "context_note": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "work_session": {
    "label": "Work Session",
    "fields": {
      "timestamp_start": {
        "type": "datetime",
        "required": true
      },
      "timestamp_end": {
        "type": "datetime"
      },
      "duration_minutes": {
        "type": "number"
      },
      "task_type": {
        "type": "select",
        "options": [
          "professional",
          "personal",
          "academic",
          "freelance",
          "creative",
          "admin",
          "other"
        ]
      },
      "task_name": {
        "type": "text",
        "required": true
      },
      "project": {
        "type": "text"
      },
      "session_description": {
        "type": "textarea"
      },
      "focus_level": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "output_score": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "distraction_count": {
        "type": "number"
      },
      "distraction_description": {
        "type": "textarea"
      },
      "pauses": {
        "type": "array",
        "label": "Pauses",
        "itemFields": {
          "pause_start_minutes": {
            "type": "number",
            "label": "Start (min into session)"
          },
          "pause_end_minutes": {
            "type": "number",
            "label": "End (min into session)"
          },
          "duration_minutes": {
            "type": "number"
          },
          "reason": {
            "type": "text"
          }
        }
      },
      "total_pause_minutes": {
        "type": "number"
      },
      "total_productive_minutes": {
        "type": "number",
        "label": "Total Productive Time (mins)"
      },
      "mental_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "physical_energy": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "clarity": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "motivation": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "confidence": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "tiredness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "frustration": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "stress": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "sleepiness": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "location": {
        "type": "select",
        "options": [
          "work room",
          "bedroom",
          "living room",
          "office",
          "cafe",
          "library",
          "other"
        ]
      },
      "session_quality": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "habit_task_manager": {
    "label": "Habit or Task Manager",
    "fields": {
      "name": {
        "type": "text",
        "required": true
      },
      "description": {
        "type": "textarea"
      },
      "category": {
        "type": "text"
      },
      "subcategory": {
        "type": "text"
      },
      "project": {
        "type": "text"
      },
      "subproject": {
        "type": "text"
      },
      "priority": {
        "type": "scale",
        "min": 1,
        "max": 5
      },
      "completionType": {
        "type": "select",
        "options": [
          "boolean",
          "duration",
          "percentage",
          "fraction",
          "count",
          "custom"
        ]
      },
      "targetValue": {
        "type": "number"
      },
      "targetUnit": {
        "type": "text"
      },
      "frequency_type": {
        "type": "select",
        "options": [
          "daily",
          "weekly",
          "monthly",
          "yearly",
          "custom"
        ]
      },
      "frequency_timesPerPeriod": {
        "type": "number"
      },
      "frequency_customDays": {
        "type": "tags",
        "placeholder": "e.g. monday, thursday"
      },
      "timeBlock_enabled": {
        "type": "boolean"
      },
      "timeBlock_startTime": {
        "type": "time"
      },
      "timeBlock_endTime": {
        "type": "time"
      },
      "streak_current": {
        "type": "number"
      },
      "streak_longest": {
        "type": "number"
      },
      "streak_lastLoggedDate": {
        "type": "date"
      },
      "streak_allowedMissesBeforeBreak": {
        "type": "number"
      },
      "streak_currentConsecutiveMisses": {
        "type": "number"
      },
      "procrastination_enabled": {
        "type": "boolean"
      },
      "procrastination_totalMissedPeriods": {
        "type": "number"
      },
      "procrastination_missedDates": {
        "type": "tags",
        "placeholder": "YYYY-MM-DD format"
      },
      "procrastination_longestMissStreak": {
        "type": "number"
      },
      "procrastination_currentMissStreak": {
        "type": "number"
      },
      "isActive": {
        "type": "boolean"
      },
      "createdAt": {
        "type": "datetime"
      },
      "updatedAt": {
        "type": "datetime"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "todo_system": {
    "label": "Todo System",
    "fields": {
      "title": {
        "type": "text",
        "required": true
      },
      "description": {
        "type": "textarea"
      },
      "category": {
        "type": "text"
      },
      "subcategory": {
        "type": "text"
      },
      "project": {
        "type": "text"
      },
      "subproject": {
        "type": "text"
      },
      "priority": {
        "type": "scale",
        "min": 1,
        "max": 5
      },
      "labels": {
        "type": "tags"
      },
      "deadline_type": {
        "type": "select",
        "options": [
          "exact",
          "within",
          "range",
          "anytime"
        ]
      },
      "deadline_exactDate": {
        "type": "date"
      },
      "deadline_windowStart": {
        "type": "date"
      },
      "deadline_windowEnd": {
        "type": "date"
      },
      "estimatedStartTime": {
        "type": "datetime"
      },
      "estimatedDuration": {
        "type": "number",
        "label": "Estimated Duration (minutes)"
      },
      "estimatedEndTime": {
        "type": "datetime"
      },
      "actualStartTime": {
        "type": "datetime"
      },
      "actualEndTime": {
        "type": "datetime"
      },
      "actualDuration": {
        "type": "number",
        "label": "Actual Duration (minutes)"
      },
      "status": {
        "type": "select",
        "options": [
          "todo",
          "in_progress",
          "completed",
          "procrastinated"
        ]
      },
      "completedAt": {
        "type": "datetime"
      },
      "procrastination_isActive": {
        "type": "boolean"
      },
      "procrastination_triggeredBy": {
        "type": "select",
        "options": [
          "auto",
          "manual"
        ]
      },
      "procrastination_clockStartedAt": {
        "type": "datetime"
      },
      "procrastination_totalMinutes": {
        "type": "number"
      },
      "procrastination_manuallyFlagged": {
        "type": "boolean"
      },
      "procrastination_checkpoints": {
        "type": "array",
        "label": "Procrastination Checkpoints",
        "itemFields": {
          "recordedAt": {
            "type": "datetime"
          },
          "minutesSoFar": {
            "type": "number"
          }
        }
      },
      "sessions": {
        "type": "array",
        "label": "Work Sessions",
        "itemFields": {
          "start": {
            "type": "datetime"
          },
          "end": {
            "type": "datetime"
          },
          "duration": {
            "type": "number",
            "label": "Duration (min)"
          }
        }
      },
      "createdAt": {
        "type": "datetime"
      },
      "updatedAt": {
        "type": "datetime"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  },
  "daily_summary": {
    "label": "Daily Summary",
    "fields": {
      "date": {
        "type": "date",
        "required": true
      },
      "overall_day_rating": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "highlights": {
        "type": "textarea",
        "label": "Highlights of the day"
      },
      "lowlights": {
        "type": "textarea",
        "label": "Lowlights / Challenges"
      },
      "tasks_completed": {
        "type": "number"
      },
      "major_achievements": {
        "type": "tags"
      },
      "habits_completed_percent": {
        "type": "number",
        "label": "Habits Completion %"
      },
      "mood_average": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "energy_average": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "stress_average": {
        "type": "scale",
        "min": 0,
        "max": 10
      },
      "notes": {
        "type": "textarea"
      },
      "custom_metrics": {
        "type": "array",
        "label": "Custom Metrics",
        "itemFields": {
          "metric_name": {
            "type": "text",
            "label": "Metric Name"
          },
          "value": {
            "type": "text",
            "label": "Value"
          },
          "intensity_or_scale": {
            "type": "scale",
            "min": 0,
            "max": 10,
            "label": "Intensity / Scale"
          },
          "notes": {
            "type": "textarea",
            "label": "Notes"
          }
        }
      }
    }
  }
};
