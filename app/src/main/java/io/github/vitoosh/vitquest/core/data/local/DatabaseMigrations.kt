package io.github.vitoosh.vitquest.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change must add an explicit migration here (with a test).
 * Destructive fallback is intentionally not used so user data is never wiped
 * by an app update. SQL is copied from the exported schema in app/schemas/.
 */
object DatabaseMigrations {

    /** v2 (Phase 2): daily step history and the phone step-counter checkpoint. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `daily_steps` (`date` TEXT NOT NULL, `source` TEXT NOT NULL, " +
                    "`steps` INTEGER NOT NULL, `updated_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`date`, `source`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `step_sensor_state` (`id` INTEGER NOT NULL, `boot_count` INTEGER, " +
                    "`counter` INTEGER NOT NULL, `recorded_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    /**
     * v3 (Phase 3): biological sex and manual calorie target on the profile, plus
     * foods, food diary, recipes, meal plan, shopping list, weight, measurements
     * and water. An existing profile weight becomes the first weight entry.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "ALTER TABLE `user_profile` ADD COLUMN `sex` TEXT",
                "ALTER TABLE `goals` ADD COLUMN `daily_calorie_target` INTEGER",
                "CREATE TABLE IF NOT EXISTS `foods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `seed_key` TEXT, `name_id` TEXT NOT NULL, `name_en` TEXT NOT NULL, `category` TEXT NOT NULL, `kcal_per_100g` REAL NOT NULL, `protein_per_100g` REAL, `carbs_per_100g` REAL, `fat_per_100g` REAL, `is_custom` INTEGER NOT NULL, `is_favorite` INTEGER NOT NULL, `source` TEXT, `created_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_foods_seed_key` ON `foods` (`seed_key`)",
                "CREATE TABLE IF NOT EXISTS `food_servings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `food_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `label_id` TEXT NOT NULL, `label_en` TEXT NOT NULL, `grams` REAL NOT NULL, FOREIGN KEY(`food_id`) REFERENCES `foods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_food_servings_food_id` ON `food_servings` (`food_id`)",
                "CREATE TABLE IF NOT EXISTS `food_log_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `meal` TEXT NOT NULL, `food_id` INTEGER, `name_id` TEXT NOT NULL, `name_en` TEXT NOT NULL, `grams` REAL NOT NULL, `quantity` REAL NOT NULL, `serving_label_id` TEXT, `serving_label_en` TEXT, `kcal` REAL NOT NULL, `protein_g` REAL, `carbs_g` REAL, `fat_g` REAL, `created_at_epoch_ms` INTEGER NOT NULL, FOREIGN KEY(`food_id`) REFERENCES `foods`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_food_log_entries_date` ON `food_log_entries` (`date`)",
                "CREATE INDEX IF NOT EXISTS `index_food_log_entries_food_id` ON `food_log_entries` (`food_id`)",
                "CREATE TABLE IF NOT EXISTS `recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `seed_key` TEXT, `name_id` TEXT NOT NULL, `name_en` TEXT NOT NULL, `description_id` TEXT NOT NULL, `description_en` TEXT NOT NULL, `servings` INTEGER NOT NULL, `prep_minutes` INTEGER NOT NULL, `cook_minutes` INTEGER NOT NULL, `tags` TEXT NOT NULL, `steps_id` TEXT NOT NULL, `steps_en` TEXT NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_recipes_seed_key` ON `recipes` (`seed_key`)",
                "CREATE TABLE IF NOT EXISTS `recipe_ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipe_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `name_id` TEXT NOT NULL, `name_en` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit_id` TEXT NOT NULL, `unit_en` TEXT NOT NULL, `grams` REAL NOT NULL, `substitute_id` TEXT, `substitute_en` TEXT, `kcal` REAL NOT NULL, `protein_g` REAL, `carbs_g` REAL, `fat_g` REAL, FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipe_id` ON `recipe_ingredients` (`recipe_id`)",
                "CREATE TABLE IF NOT EXISTS `meal_plan_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `meal` TEXT NOT NULL, `recipe_id` INTEGER NOT NULL, `servings` REAL NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL, FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_meal_plan_entries_date` ON `meal_plan_entries` (`date`)",
                "CREATE INDEX IF NOT EXISTS `index_meal_plan_entries_recipe_id` ON `meal_plan_entries` (`recipe_id`)",
                "CREATE TABLE IF NOT EXISTS `shopping_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name_id` TEXT NOT NULL, `name_en` TEXT NOT NULL, `quantity` REAL, `unit_id` TEXT, `unit_en` TEXT, `checked` INTEGER NOT NULL, `from_plan` INTEGER NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `weight_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `weight_kg` REAL NOT NULL, `note` TEXT, `updated_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_weight_entries_date` ON `weight_entries` (`date`)",
                "CREATE TABLE IF NOT EXISTS `body_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `waist_cm` REAL, `hip_cm` REAL, `chest_cm` REAL, `arm_cm` REAL, `thigh_cm` REAL, `updated_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_body_measurements_date` ON `body_measurements` (`date`)",
                "CREATE TABLE IF NOT EXISTS `water_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `amount_ml` INTEGER NOT NULL, `logged_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE INDEX IF NOT EXISTS `index_water_logs_date` ON `water_logs` (`date`)",
            ).forEach(db::execSQL)
            db.execSQL(
                "INSERT INTO `weight_entries` (`date`, `weight_kg`, `note`, `updated_at_epoch_ms`) " +
                    "SELECT date(`updated_at_epoch_ms` / 1000, 'unixepoch', 'localtime'), `current_weight_kg`, NULL, " +
                    "`updated_at_epoch_ms` FROM `user_profile` WHERE `current_weight_kg` IS NOT NULL",
            )
        }
    }

    /** v4 (Phase 4): workout history and custom habits. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `workout_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `started_at_epoch_ms` INTEGER NOT NULL, `ended_at_epoch_ms` INTEGER NOT NULL, `source` TEXT NOT NULL, `focus` TEXT, `level` TEXT, `location` TEXT, `intensity` TEXT, `planned_seconds` INTEGER NOT NULL, `active_seconds` INTEGER NOT NULL, `exercises_completed` INTEGER NOT NULL, `exercises_total` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `estimated_kcal` REAL, `note` TEXT)",
                "CREATE INDEX IF NOT EXISTS `index_workout_sessions_date` ON `workout_sessions` (`date`)",
                "CREATE TABLE IF NOT EXISTS `workout_session_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `session_id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `exercise_key` TEXT NOT NULL, `name_id` TEXT NOT NULL, `name_en` TEXT NOT NULL, `planned_seconds` INTEGER NOT NULL, `worked_seconds` INTEGER NOT NULL, `status` TEXT NOT NULL, FOREIGN KEY(`session_id`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_workout_session_exercises_session_id` ON `workout_session_exercises` (`session_id`)",
                "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `target_days_per_week` INTEGER NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `habit_check_ins` (`habit_id` INTEGER NOT NULL, `date` TEXT NOT NULL, PRIMARY KEY(`habit_id`, `date`), FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                "CREATE INDEX IF NOT EXISTS `index_habit_check_ins_date` ON `habit_check_ins` (`date`)",
            ).forEach(db::execSQL)
        }
    }

    /** v5 (Phase 5): XP and Reward Credit ledger (credits removed in v6), rewards, redemptions, real-money budget and reminder settings. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "CREATE TABLE IF NOT EXISTS `reward_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `currency` TEXT NOT NULL, `amount` INTEGER NOT NULL, `reason` TEXT NOT NULL, `source_key` TEXT NOT NULL, `date` TEXT NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_transactions_currency_source_key` ON `reward_transactions` (`currency`, `source_key`)",
                "CREATE INDEX IF NOT EXISTS `index_reward_transactions_date` ON `reward_transactions` (`date`)",
                "CREATE TABLE IF NOT EXISTS `rewards` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `seed_key` TEXT, `name` TEXT NOT NULL, `description` TEXT, `type` TEXT NOT NULL, `credit_cost` INTEGER NOT NULL, `price_rupiah` INTEGER, `wishlist` INTEGER NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_rewards_seed_key` ON `rewards` (`seed_key`)",
                "CREATE TABLE IF NOT EXISTS `redemptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `request_id` TEXT NOT NULL, `reward_id` INTEGER, `reward_name` TEXT NOT NULL, `credit_cost` INTEGER NOT NULL, `redeemed_at_epoch_ms` INTEGER NOT NULL, `date` TEXT NOT NULL, `status` TEXT NOT NULL, FOREIGN KEY(`reward_id`) REFERENCES `rewards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_redemptions_request_id` ON `redemptions` (`request_id`)",
                "CREATE INDEX IF NOT EXISTS `index_redemptions_reward_id` ON `redemptions` (`reward_id`)",
                "CREATE TABLE IF NOT EXISTS `budget_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `kind` TEXT NOT NULL, `amount_rupiah` INTEGER NOT NULL, `reward_id` INTEGER, `note` TEXT, `created_at_epoch_ms` INTEGER NOT NULL, FOREIGN KEY(`reward_id`) REFERENCES `rewards`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE INDEX IF NOT EXISTS `index_budget_entries_date` ON `budget_entries` (`date`)",
                "CREATE INDEX IF NOT EXISTS `index_budget_entries_reward_id` ON `budget_entries` (`reward_id`)",
                "CREATE TABLE IF NOT EXISTS `reminder_settings` (`type` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `interval_minutes` INTEGER, `time_minutes` INTEGER, `days` TEXT NOT NULL, PRIMARY KEY(`type`))",
                "CREATE TABLE IF NOT EXISTS `reminder_window` (`id` INTEGER NOT NULL, `active_start` INTEGER, `active_end` INTEGER, `quiet_start` INTEGER, `quiet_end` INTEGER, PRIMARY KEY(`id`))",
            ).forEach(db::execSQL)
        }
    }

    /**
     * v6: Reward Credits removed. Credit lines are dropped, rewards move to `reward_items`
     * without a credit cost, and redemptions that were not cancelled become history.
     * Children are rebuilt before `rewards` is dropped so no foreign key is broken.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "DELETE FROM `reward_transactions` WHERE `currency` = 'CREDIT'",
                "CREATE TABLE IF NOT EXISTS `reward_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `seed_key` TEXT, `name` TEXT NOT NULL, `description` TEXT, `type` TEXT NOT NULL, `price_rupiah` INTEGER, `wishlist` INTEGER NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL)",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_items_seed_key` ON `reward_items` (`seed_key`)",
                "INSERT INTO `reward_items` (`id`, `seed_key`, `name`, `description`, `type`, `price_rupiah`, `wishlist`, `created_at_epoch_ms`) " +
                    "SELECT `id`, `seed_key`, `name`, `description`, `type`, `price_rupiah`, `wishlist`, `created_at_epoch_ms` FROM `rewards`",
                "CREATE TABLE IF NOT EXISTS `reward_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `request_id` TEXT NOT NULL, `reward_id` INTEGER, `reward_name` TEXT NOT NULL, `price_paid_rupiah` INTEGER, `budget_entry_id` INTEGER, `obtained_at_epoch_ms` INTEGER NOT NULL, `date` TEXT NOT NULL, FOREIGN KEY(`reward_id`) REFERENCES `reward_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_history_request_id` ON `reward_history` (`request_id`)",
                "CREATE INDEX IF NOT EXISTS `index_reward_history_reward_id` ON `reward_history` (`reward_id`)",
                "INSERT INTO `reward_history` (`request_id`, `reward_id`, `reward_name`, `price_paid_rupiah`, `budget_entry_id`, `obtained_at_epoch_ms`, `date`) " +
                    "SELECT `request_id`, `reward_id`, `reward_name`, NULL, NULL, `redeemed_at_epoch_ms`, `date` FROM `redemptions` WHERE `status` = 'REDEEMED'",
                "CREATE TABLE IF NOT EXISTS `budget_entries_v6` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `kind` TEXT NOT NULL, `amount_rupiah` INTEGER NOT NULL, `reward_id` INTEGER, `note` TEXT, `created_at_epoch_ms` INTEGER NOT NULL, FOREIGN KEY(`reward_id`) REFERENCES `reward_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
                "INSERT INTO `budget_entries_v6` (`id`, `date`, `kind`, `amount_rupiah`, `reward_id`, `note`, `created_at_epoch_ms`) " +
                    "SELECT `id`, `date`, `kind`, `amount_rupiah`, `reward_id`, `note`, `created_at_epoch_ms` FROM `budget_entries`",
                "DROP TABLE `budget_entries`",
                "ALTER TABLE `budget_entries_v6` RENAME TO `budget_entries`",
                "CREATE INDEX IF NOT EXISTS `index_budget_entries_date` ON `budget_entries` (`date`)",
                "CREATE INDEX IF NOT EXISTS `index_budget_entries_reward_id` ON `budget_entries` (`reward_id`)",
                "DROP TABLE `redemptions`",
                "DROP TABLE `rewards`",
            ).forEach(db::execSQL)
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
