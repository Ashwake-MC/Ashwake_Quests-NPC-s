package com.ashwake.quests.npcs.client;

public final class GuidanceQuestState {
    private static boolean accepted;
    private static boolean completed;

    private GuidanceQuestState() {
    }

    public static boolean isAccepted() {
        return accepted;
    }

    public static void acceptGuidanceQuest() {
        setAccepted(true);
    }

    public static void completeGuidanceQuest() {
        setCompleted(true);
    }

    public static void setAccepted(boolean accepted) {
        GuidanceQuestState.accepted = accepted;
    }

    public static boolean isCompleted() {
        return completed;
    }

    public static void setCompleted(boolean completed) {
        GuidanceQuestState.completed = completed;
    }
}
