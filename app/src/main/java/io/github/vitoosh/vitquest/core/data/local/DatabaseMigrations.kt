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

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
