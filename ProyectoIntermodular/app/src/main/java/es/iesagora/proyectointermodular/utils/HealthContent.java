package es.iesagora.proyectointermodular.utils;

import java.util.Random;

import es.iesagora.proyectointermodular.R;

public class HealthContent {

    public static class ExerciseNode {
        public String name;
        public String duration;
        public int imageResId;

        public ExerciseNode(String name, String duration, int imageResId) {
            this.name = name;
            this.duration = duration;
            this.imageResId = imageResId;
        }
    }

    private static final int PLACEHOLDER = R.drawable.icph1;
    private static final int PLACEHOLDER1 = R.drawable.icph2;
    private static final int PLACEHOLDER2 = R.drawable.icph3;
    private static final int PLACEHOLDER3 = R.drawable.icph4;
    private static final int PLACEHOLDER4 = R.drawable.icph5;
    private static final int PLACEHOLDER5 = R.drawable.icph6;
    private static final int PLACEHOLDER6 = R.drawable.icph7;
    private static final int PLACEHOLDER7 = R.drawable.icph8;
    private static final int PLACEHOLDER8 = R.drawable.icph9;
    private static final int PLACEHOLDER9 = R.drawable.icph10;
    private static final int PLACEHOLDER10 = R.drawable.icph11;
    private static final int PLACEHOLDER11 = R.drawable.icph12;
    private static final int PLACEHOLDER12 = R.drawable.icph13;
    private static final int PLACEHOLDER13 = R.drawable.icph14;
    private static final int PLACEHOLDER14 = R.drawable.icph15;
    private static final int PLACEHOLDER15 = R.drawable.icph16;

    public static final ExerciseNode[] FUERZA = {
            new ExerciseNode("Plancha abdominal", "60 seg", PLACEHOLDER),
            new ExerciseNode("Sentadilla isométrica", "45 seg", PLACEHOLDER1),
            new ExerciseNode("Flexiones diamante", "15 reps", PLACEHOLDER2),
            new ExerciseNode("Puente de glúteo", "20 reps", PLACEHOLDER3)
    };

    public static final ExerciseNode[] RESISTENCIA = {
            new ExerciseNode("Jumping Jacks", "60 seg", PLACEHOLDER4),
            new ExerciseNode("Burpees suaves", "10 reps", PLACEHOLDER5),
            new ExerciseNode("Skipping", "45 seg", PLACEHOLDER6),
            new ExerciseNode("Sombra de boxeo", "2 min", PLACEHOLDER7)
    };

    public static final ExerciseNode[] FLEXIBILIDAD = {
            new ExerciseNode("Gato-Camello", "10 reps", PLACEHOLDER8),
            new ExerciseNode("Estiramiento de cobra", "30 seg", PLACEHOLDER9),
            new ExerciseNode("Rotación torácica", "10 reps/lado", PLACEHOLDER10),
            new ExerciseNode("Apertura de cadera", "45 seg/lado", PLACEHOLDER11)
    };

    public static final ExerciseNode[] AGILIDAD = {
            new ExerciseNode("Equilibrio a una pierna", "30 seg/lado", PLACEHOLDER12),
            new ExerciseNode("Toque de talón cruzado", "20 reps", PLACEHOLDER13),
            new ExerciseNode("Skater jumps", "15 reps/lado", PLACEHOLDER14)
    };

    private static final Random random = new Random();

    public static ExerciseNode getRandomExercise(ExerciseNode[] array) {
        return array[random.nextInt(array.length)];
    }
}
