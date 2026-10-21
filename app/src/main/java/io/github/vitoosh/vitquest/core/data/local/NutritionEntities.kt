package io.github.vitoosh.vitquest.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Built-in foods have a stable seed_key so updated reference values replace old ones; custom foods have none. */
@Entity(tableName = "foods", indices = [Index(value = ["seed_key"], unique = true)])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "seed_key") val seedKey: String?,
    @ColumnInfo(name = "name_id") val nameId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    val category: String,
    @ColumnInfo(name = "kcal_per_100g") val kcalPer100g: Double,
    @ColumnInfo(name = "protein_per_100g") val proteinPer100g: Double?,
    @ColumnInfo(name = "carbs_per_100g") val carbsPer100g: Double?,
    @ColumnInfo(name = "fat_per_100g") val fatPer100g: Double?,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    val source: String?,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

@Entity(
    tableName = "food_servings",
    foreignKeys = [ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["food_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("food_id")],
)
data class FoodServingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "food_id") val foodId: Long,
    val position: Int,
    @ColumnInfo(name = "label_id") val labelId: String,
    @ColumnInfo(name = "label_en") val labelEn: String,
    val grams: Double,
)

/** Name and nutrients are snapshots, so deleting or editing a food never changes past days. */
@Entity(
    tableName = "food_log_entries",
    foreignKeys = [ForeignKey(entity = FoodEntity::class, parentColumns = ["id"], childColumns = ["food_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("date"), Index("food_id"), Index("photo_id")],
)
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val meal: String,
    @ColumnInfo(name = "food_id") val foodId: Long?,
    @ColumnInfo(name = "name_id") val nameId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    val grams: Double,
    val quantity: Double,
    @ColumnInfo(name = "serving_label_id") val servingLabelId: String?,
    @ColumnInfo(name = "serving_label_en") val servingLabelEn: String?,
    val kcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double?,
    @ColumnInfo(name = "carbs_g") val carbsG: Double?,
    @ColumnInfo(name = "fat_g") val fatG: Double?,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
    /** Set when the entry came from a food photo; the photo is [FoodPhotoEntity] with this id. */
    @ColumnInfo(name = "photo_id") val photoId: Long? = null,
)

/**
 * A small copy of a food photo, stored as `food_photos/<id>.jpg` in app storage.
 * Entries point at it without a foreign key; the photo is deleted with its last entry.
 */
@Entity(tableName = "food_photos")
data class FoodPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

@Entity(tableName = "recipes", indices = [Index(value = ["seed_key"], unique = true)])
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "seed_key") val seedKey: String?,
    @ColumnInfo(name = "name_id") val nameId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "description_id") val descriptionId: String,
    @ColumnInfo(name = "description_en") val descriptionEn: String,
    val servings: Int,
    @ColumnInfo(name = "prep_minutes") val prepMinutes: Int,
    @ColumnInfo(name = "cook_minutes") val cookMinutes: Int,
    /** Comma-separated RecipeTag names. */
    val tags: String,
    /** Steps separated by newlines. */
    @ColumnInfo(name = "steps_id") val stepsId: String,
    @ColumnInfo(name = "steps_en") val stepsEn: String,
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipe_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipe_id")],
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val position: Int,
    @ColumnInfo(name = "name_id") val nameId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    val quantity: Double,
    @ColumnInfo(name = "unit_id") val unitId: String,
    @ColumnInfo(name = "unit_en") val unitEn: String,
    val grams: Double,
    @ColumnInfo(name = "substitute_id") val substituteId: String?,
    @ColumnInfo(name = "substitute_en") val substituteEn: String?,
    val kcal: Double,
    @ColumnInfo(name = "protein_g") val proteinG: Double?,
    @ColumnInfo(name = "carbs_g") val carbsG: Double?,
    @ColumnInfo(name = "fat_g") val fatG: Double?,
)

@Entity(
    tableName = "meal_plan_entries",
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipe_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("date"), Index("recipe_id")],
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val meal: String,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val servings: Double,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name_id") val nameId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    /** Amount and unit in both languages; null for items the user typed without an amount. */
    val quantity: Double?,
    @ColumnInfo(name = "unit_id") val unitId: String?,
    @ColumnInfo(name = "unit_en") val unitEn: String?,
    val checked: Boolean,
    @ColumnInfo(name = "from_plan") val fromPlan: Boolean,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
)

/** One weight per day; logging again on the same day replaces it. */
@Entity(tableName = "weight_entries", indices = [Index(value = ["date"], unique = true)])
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    val note: String?,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)

@Entity(tableName = "body_measurements", indices = [Index(value = ["date"], unique = true)])
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    @ColumnInfo(name = "waist_cm") val waistCm: Double?,
    @ColumnInfo(name = "hip_cm") val hipCm: Double?,
    @ColumnInfo(name = "chest_cm") val chestCm: Double?,
    @ColumnInfo(name = "arm_cm") val armCm: Double?,
    @ColumnInfo(name = "thigh_cm") val thighCm: Double?,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
)

@Entity(tableName = "water_logs", indices = [Index("date")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    @ColumnInfo(name = "amount_ml") val amountMl: Int,
    @ColumnInfo(name = "logged_at_epoch_ms") val loggedAtEpochMs: Long,
)

/** Room relation: a food with its serving sizes. */
data class FoodWithServings(
    @androidx.room.Embedded val food: FoodEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "food_id")
    val servings: List<FoodServingEntity>,
)

data class RecipeWithIngredients(
    @androidx.room.Embedded val recipe: RecipeEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "recipe_id")
    val ingredients: List<RecipeIngredientEntity>,
)

/** A plan entry joined with its recipe's names and per-serving nutrients. */
data class MealPlanWithRecipe(
    @androidx.room.Embedded val entry: MealPlanEntity,
    @androidx.room.Relation(parentColumn = "recipe_id", entityColumn = "id", entity = RecipeEntity::class)
    val recipe: RecipeWithIngredients,
)
