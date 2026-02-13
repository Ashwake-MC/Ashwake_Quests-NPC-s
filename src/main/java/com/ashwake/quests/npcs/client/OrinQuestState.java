package com.ashwake.quests.npcs.client;

public final class OrinQuestState {
    private static int stage;
    private static boolean waystoneGiven;
    private static boolean visitedPersonal;
    private static boolean questCompleted;

    private OrinQuestState() {
    }

    public static void setState(int stage, boolean waystoneGiven, boolean visitedPersonal, boolean questCompleted) {
        OrinQuestState.stage = stage;
        OrinQuestState.waystoneGiven = waystoneGiven;
        OrinQuestState.visitedPersonal = visitedPersonal;
        OrinQuestState.questCompleted = questCompleted;
    }

    public static int getStage() {
        return stage;
    }

    public static void setStage(int stage) {
        OrinQuestState.stage = stage;
    }

    public static boolean isQuest1Accepted() {
        return stage >= 1;
    }

    public static boolean isQuest1Completed() {
        return stage >= 2;
    }

    public static boolean isQuest2Accepted() {
        return stage >= 3;
    }

    public static boolean isQuestCompleted() {
        return questCompleted || stage >= 4;
    }

    public static boolean hasWaystone() {
        return waystoneGiven;
    }

    public static boolean isVisitedPersonal() {
        return visitedPersonal;
    }

    public static void reset() {
        stage = 0;
        waystoneGiven = false;
        visitedPersonal = false;
        questCompleted = false;
    }
}
