package Enums;

public enum Effects {
  AFTERBURNER(1),
  ASTEROID_FIELD(2),
  GAS_CLOUD(4),
  SUPER_FOOD(8),
  SHIELD(16);

  public final Integer value;

  Effects(Integer value) {
    this.value = value;
  }

  public int getEffects(){
    return this.value;
  }
}
