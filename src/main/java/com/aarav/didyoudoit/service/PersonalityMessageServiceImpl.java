package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.model.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of {@link PersonalityMessageService} containing a rich catalog
 * of 160+ unique, randomized nagging and accountability messages across
 * Gentle, Strict, Sarcastic, and Aggressive styles.
 * Implements FR-06, FR-07, FR-15, FR-16, BR-02, BR-12.
 */
public class PersonalityMessageServiceImpl implements PersonalityMessageService {

    @Override
    public NagMessage generateMessage(Task task, PersonalityType personality, int minutesOverdue) {
        Objects.requireNonNull(task, "task cannot be null");
        PersonalityType activePersonality = Objects.requireNonNullElse(personality, PersonalityType.SARCASTIC);

        EscalationLevel level = calculateEscalationLevel(minutesOverdue, task.getNagCount());
        String customMsg = task.getCustomNagMessage();

        if (customMsg != null && !customMsg.isBlank()) {
            String title = "DidYouDoIt? - " + task.getTitle();
            String body = customMsg.trim() + " (" + minutesOverdue + "m overdue)";
            return new NagMessage(title, body, level);
        }

        String headline = getRandomHeadline(task.getTitle(), activePersonality, level);
        String body = getRandomBody(task.getTitle(), activePersonality, level, minutesOverdue);

        return new NagMessage(headline, body, level);
    }

    private EscalationLevel calculateEscalationLevel(int minutesOverdue, int nagCount) {
        if (minutesOverdue > 45 || nagCount >= 3) {
            return EscalationLevel.CRITICAL;
        } else if (minutesOverdue > 15 || nagCount >= 2) {
            return EscalationLevel.WARN;
        } else if (minutesOverdue > 0 || nagCount >= 1) {
            return EscalationLevel.NUDGE;
        } else {
            return EscalationLevel.INITIAL;
        }
    }

    // -------------------------------------------------------------------------
    // HEADLINE POOLS (10+ variations per personality x level)
    // -------------------------------------------------------------------------

    private String getRandomHeadline(String title, PersonalityType personality, EscalationLevel level) {
        List<String> pool = switch (personality) {
            case GENTLE -> switch (level) {
                case INITIAL -> List.of(
                        "Time to focus: " + title,
                        "Ready when you are: " + title,
                        "Gentle moment for: " + title,
                        "Your scheduled task: " + title,
                        "A little time for: " + title,
                        "Time to shine: " + title,
                        "A calm start for: " + title,
                        "One step at a time: " + title,
                        "Mindful reminder: " + title,
                        "You've got this: " + title
                );
                case NUDGE -> List.of(
                        "Gentle Reminder: " + title,
                        "Still waiting gently: " + title,
                        "A friendly nudge: " + title,
                        "Don't forget: " + title,
                        "Checking in with you: " + title,
                        "Small progress today: " + title,
                        "Just a warm thought for: " + title,
                        "A little tap on the shoulder: " + title,
                        "Take a breath and do: " + title,
                        "Keep your rhythm: " + title
                );
                case WARN -> List.of(
                        "Still pending: " + title,
                        "Thinking of you and: " + title,
                        "A little extra encouragement for: " + title,
                        "You'll feel lighter after: " + title,
                        "Don't lose momentum on: " + title,
                        "Take 5 minutes for: " + title,
                        "Almost there, please try: " + title,
                        "Be kind to tomorrow by finishing: " + title,
                        "Still hoping you'll start: " + title,
                        "Give yourself a win with: " + title
                );
                case CRITICAL -> List.of(
                        "Needs your attention: " + title,
                        "Don't give up on: " + title,
                        "Your future self is waiting: " + title,
                        "A final gentle appeal: " + title,
                        "Reclaim your day with: " + title,
                        "It's never too late for: " + title,
                        "Please take care of: " + title,
                        "You deserve the relief of finishing: " + title,
                        "Breathe in, let's complete: " + title,
                        "Finish strong today: " + title
                );
            };
            case STRICT -> switch (level) {
                case INITIAL -> List.of(
                        "Due Now: " + title,
                        "Scheduled Execution: " + title,
                        "Commence Task: " + title,
                        "Target Time Reached: " + title,
                        "Action Required: " + title,
                        "Standard Operating Procedure: " + title,
                        "Scheduled Milestone: " + title,
                        "Deadline Reached: " + title,
                        "Commence: " + title,
                        "Direct Order: " + title
                );
                case NUDGE -> List.of(
                        "Overdue: " + title,
                        "Pending Compliance: " + title,
                        "Unresolved Task: " + title,
                        "Execution Lagging: " + title,
                        "Deadline Exceeded: " + title,
                        "Prompt Attention Needed: " + title,
                        "Accountability Call: " + title,
                        "Action Deficit: " + title,
                        "Status Check: " + title,
                        "Overdue Warning: " + title
                );
                case WARN -> List.of(
                        "Urgent Attention: " + title,
                        "Discipline Breakdown: " + title,
                        "Significant Delay: " + title,
                        "Unacceptable Postponement: " + title,
                        "Immediate Action Demanded: " + title,
                        "Protocol Violation: " + title,
                        "Zero Excuses Permitted: " + title,
                        "Escalated Priority: " + title,
                        "High Risk Overdue: " + title,
                        "Compliance Required: " + title
                );
                case CRITICAL -> List.of(
                        "CRITICAL DEADLINE: " + title,
                        "SEVERE ESCALATION: " + title,
                        "MAXIMUM DEFAULT: " + title,
                        "TERMINATION OF GRACE PERIOD: " + title,
                        "IMMEDIATE RESOLUTION REQUIRED: " + title,
                        "UNACCEPTABLE DEFICIT: " + title,
                        "ACCOUNTABILITY FAILURE: " + title,
                        "EMERGENCY OVERDUE: " + title,
                        "EXECUTE WITHOUT DELAY: " + title,
                        "FINAL WARNING: " + title
                );
            };
            case SARCASTIC -> switch (level) {
                case INITIAL -> List.of(
                        "Look who's due: " + title,
                        "Oh hey, remember this?: " + title,
                        "A wild task appeared: " + title,
                        "Shocking development: " + title,
                        "Look at that, it's time for: " + title,
                        "Spoiler alert: it's due: " + title,
                        "Plot twist: you scheduled: " + title,
                        "Calendar says hello: " + title,
                        "Guess what time it is: " + title,
                        "Surprise, responsibility calls: " + title
                );
                case NUDGE -> List.of(
                        "Still ignoring: " + title + "?",
                        "Are we playing hide and seek with: " + title + "?",
                        "Still here waiting: " + title,
                        "Bold strategy on: " + title,
                        "Ghosting your own task: " + title,
                        "Did you forget your hands?: " + title,
                        "Task is feeling lonely: " + title,
                        "Did you get lost on the way to: " + title + "?",
                        "Fascinating delay on: " + title,
                        "Still not done: " + title
                );
                case WARN -> List.of(
                        "Procrastination Champion: " + title,
                        "Olympic-level stalling on: " + title,
                        "Still finding creative excuses for: " + title + "?",
                        "Your keyboard misses you on: " + title,
                        "A masterclass in avoidance: " + title,
                        "Are you admiring the wallpaper instead of: " + title + "?",
                        "Statue impersonation over: " + title,
                        "Did you expect elves to finish: " + title + "?",
                        "Procrastination milestone unlocked: " + title,
                        "Still waiting for motivation to hit on: " + title + "?"
                );
                case CRITICAL -> List.of(
                        "RIP Productivity: " + title,
                        "Did you think this was going to finish itself?: " + title,
                        "Museum piece overdue: " + title,
                        "Archeologists just discovered: " + title,
                        "A tragedy in multiple acts: " + title,
                        "Congratulations on avoiding: " + title,
                        "Earth completed another orbit while waiting for: " + title,
                        "The procrastination hall of fame awaits: " + title,
                        "Is this a modern art installation?: " + title,
                        "Stop reading this and do: " + title
                );
            };
            case AGGRESSIVE -> switch (level) {
                case INITIAL -> List.of(
                        "DO IT NOW: " + title,
                        "CLOCK IS TICKING: " + title,
                        "TIME'S UP: " + title,
                        "START IMMEDIATELY: " + title,
                        "GET TO WORK: " + title,
                        "ZERO DELAY: " + title,
                        "FOCUS UP: " + title,
                        "PUT IN THE WORK: " + title,
                        "NOW OR NEVER: " + title,
                        "EXECUTE: " + title
                );
                case NUDGE -> List.of(
                        "MOVE IT: " + title,
                        "WHY ARE YOU STILL WAITING: " + title,
                        "STOP SLACKING ON: " + title,
                        "WHAT HAPPENED TO YOUR DRIVE: " + title,
                        "NO MORE WAITING: " + title,
                        "UP AND AT 'EM: " + title,
                        "GET MOVING ON: " + title,
                        "DO NOT SLEEP ON: " + title,
                        "SHOW UP FOR: " + title,
                        "GET IT DONE: " + title
                );
                case WARN -> List.of(
                        "NO EXCUSES: " + title,
                        "STOP PROCRASTINATING: " + title,
                        "ARE YOU A QUITTER?: " + title,
                        "DISCIPLINE CHECK: " + title,
                        "FACE THE GUILT OR FINISH: " + title,
                        "STEP UP RIGHT NOW: " + title,
                        "PULL YOURSELF TOGETHER FOR: " + title,
                        "WHERE IS THE EFFORT: " + title,
                        "STOP DELAYING THE INEVITABLE: " + title,
                        "FINISH THIS: " + title
                );
                case CRITICAL -> List.of(
                        "DROP EVERYTHING AND FINISH: " + title,
                        "EMERGENCY CALL: " + title,
                        "DO IT OR ADMIT DEFEAT: " + title,
                        "STOP WHAT YOU ARE DOING AND DO: " + title,
                        "UNACCEPTABLE! FINISH: " + title,
                        "THIS IS EMBARRASSING! DO: " + title,
                        "ABSOLUTELY NO MORE DELAYS FOR: " + title,
                        "WAKE UP AND EXECUTE: " + title,
                        "TOTAL COMMITMENT NEEDED FOR: " + title,
                        "ENOUGH IS ENOUGH: " + title
                );
            };
        };

        int idx = ThreadLocalRandom.current().nextInt(pool.size());
        return pool.get(idx);
    }

    // -------------------------------------------------------------------------
    // BODY POOLS (10+ variations per personality x level)
    // -------------------------------------------------------------------------

    private String getRandomBody(String title, PersonalityType personality, EscalationLevel level, int minutesOverdue) {
        List<String> pool = switch (personality) {
            case GENTLE -> switch (level) {
                case INITIAL -> List.of(
                        "You planned this for now. Take a deep breath and begin!",
                        "A small step right now will bring so much peace.",
                        "Clear away distractions and give yourself this gift of focus.",
                        "Everything starts with just five focused minutes.",
                        "You have all the strength you need to accomplish this.",
                        "Treat yourself with kindness and take action on your plan.",
                        "A gentle start is still a start. You can do this!",
                        "Believe in your rhythm. Begin when you're ready.",
                        "One little effort today brings great momentum tomorrow.",
                        "No rush, just steady, gentle focus."
                );
                case NUDGE -> List.of(
                        "Overdue by " + minutesOverdue + " minutes. Even 5 minutes of focus will help.",
                        "It's been a little while (" + minutesOverdue + "m). Gently redirecting you back.",
                        "Don't worry about being late, just start where you are right now.",
                        "A little progress right now will brighten your whole day.",
                        "Your routine is waiting for you with open arms.",
                        "Be proud of showing up, even " + minutesOverdue + " minutes later.",
                        "Gently checking in: can you take a couple of minutes to finish this?",
                        "Take a sip of water, shake off distractions, and let's do this.",
                        "The best time was scheduled; the second best time is now.",
                        "You'll feel so calm once you check this one off."
                );
                case WARN -> List.of(
                        "You will feel so much better once this is off your plate.",
                        "It has been " + minutesOverdue + "m. Break it down into tiny steps and just start.",
                        "Remember how good it felt the last time you completed this on schedule.",
                        "Holding this in your head drains energy. Let's finish it and rest.",
                        "A loving reminder that future you is counting on this moment.",
                        "You don't need perfection, you just need a few minutes of effort.",
                        "Pause what you're doing and give this task five honest minutes.",
                        "Let's protect your evening relaxation by completing this now.",
                        "Stay gentle with yourself, but firm with your intention.",
                        "You are capable of seeing this through to completion."
                );
                case CRITICAL -> List.of(
                        "It has been " + minutesOverdue + " minutes. Be kind to future you and complete it now.",
                        "This has lingered for " + minutesOverdue + "m. Close the loop and enjoy the peace.",
                        "You deserve to feel free from the weight of this pending task.",
                        "Don't let the day slip away without giving yourself this win.",
                        "Even a partial finish is better than leaving it on your mind.",
                        "Take three deep breaths, close extra tabs, and finish it right now.",
                        "You've come too far to let this habit slip away.",
                        "Turn this overdue challenge into today's quiet victory.",
                        "Let's put this to rest so you can rest tonight.",
                        "I believe in you. Let's get this done together."
                );
            };
            case STRICT -> switch (level) {
                case INITIAL -> List.of(
                        "Scheduled deadline reached. Execute immediately.",
                        "Action item active. Adhere strictly to your schedule.",
                        "Commence work now. Procrastination is not tolerated.",
                        "Target milestone is due. Do not defer.",
                        "Operational efficiency begins with punctual execution.",
                        "Your daily discipline is on trial. Begin now.",
                        "Initiate work on this task without deviation.",
                        "Follow the routine you set for yourself.",
                        "Consistency separates professionals from amateurs.",
                        "Begin work. Log completion upon delivery."
                );
                case NUDGE -> List.of(
                        "Past deadline by " + minutesOverdue + " minutes. Stop delaying and complete it.",
                        "Task is running behind schedule by " + minutesOverdue + "m. Correct course now.",
                        "Deviation from schedule detected. Re-engage immediately.",
                        "Every minute of delay compounds your daily backlog.",
                        "Discipline is doing what must be done, exactly when scheduled.",
                        "Eliminate secondary tasks and resolve this primary objective.",
                        "You are " + minutesOverdue + " minutes behind. Make up the lost time.",
                        "Excuses do not close tasks. Execution closes tasks.",
                        "Return to your designated priority without further diversion.",
                        "Accountability check failed: task remains outstanding."
                );
                case WARN -> List.of(
                        "Discipline over motivation. Finish this without further postponement.",
                        "This task is " + minutesOverdue + "m overdue. Immediate compliance is expected.",
                        "Unacceptable lag. Eliminate all distractions and execute.",
                        "You promised yourself this routine. Uphold the standard.",
                        "A delay of " + minutesOverdue + " minutes reflects a lapse in commitment.",
                        "Postponement is not a strategy. Work on it now.",
                        "Priority escalation initiated. Finish the item.",
                        "No further reminders should be necessary. Complete it.",
                        "Your standards are slipping. Restore order immediately.",
                        "Stop negotiating with yourself. Execute the plan."
                );
                case CRITICAL -> List.of(
                        "Severely overdue (" + minutesOverdue + "m). No more delays.",
                        "Severe schedule breach: " + minutesOverdue + " minutes overdue. Immediate resolution required.",
                        "Complete this task now. Zero tolerance for further neglect.",
                        "This delay is unacceptable by your own defined standards.",
                        "Halt all non-essential activities and resolve this task.",
                        "Failure to execute compromises your entire daily schedule.",
                        "Log this task as completed within the next ten minutes.",
                        "You are violating your own contract with yourself. Fix it now.",
                        "No excuses, no postponements, no second chances. Execute.",
                        "Final compliance directive: finish this task right now."
                );
            };
            case SARCASTIC -> switch (level) {
                case INITIAL -> List.of(
                        "Are you actually going to do this, or just stare at the screen?",
                        "Your calendar didn't put this here just for decorative purposes.",
                        "Surprise! The task you scheduled didn't disappear on its own.",
                        "Yes, this is that task you promised you'd do today.",
                        "Look, a wild responsibility! Will you catch it or run away?",
                        "It's scheduled for now. Groundbreaking revelation, I know.",
                        "Did you know tasks get completed much faster when you actually start them?",
                        "Time to trade scrolling for doing. Revolutionary concept.",
                        "Don't worry, the task won't bite. Probably.",
                        "A moment of silence for the excuses you're about to make."
                );
                case NUDGE -> List.of(
                        "Overdue by " + minutesOverdue + "m. Bold strategy. Let's see if magic finishes it.",
                        "You are " + minutesOverdue + "m late. Did your chair trap you?",
                        "Is there an award for ignoring notifications? Because you're winning.",
                        "I see you looking at this notification. Yes, you.",
                        "Tasks don't do themselves, despite centuries of human optimism.",
                        "Still putting it off? Let me guess, 'just checking one more tab'?",
                        "If procrastination burned calories, you'd be shredded by now.",
                        "Legend has it that someone once clicked 'Done' on time.",
                        "Are you waiting for a written invitation from the universe?",
                        "You've been ignoring this for " + minutesOverdue + "m. Impressive stamina."
                );
                case WARN -> List.of(
                        "Still putting it off? Your future self is actively facepalming right now.",
                        "It has been " + minutesOverdue + "m. Is this task personally insulting to you?",
                        "I'm not mad, just fascinated by your dedication to avoidance.",
                        "Your excuses are masterpieces of modern fiction. Finish the task anyway.",
                        "At this point, the task has aged like fine milk.",
                        "Did you really think I'd forget? I am literally built to nag you.",
                        "Stop reorganizing your desk and do the actual work.",
                        "You've spent more time dodging this than it would take to finish it.",
                        "The task is still sitting there. Mocking you silently.",
                        "If delay was an Olympic sport, you'd be on the podium right now."
                );
                case CRITICAL -> List.of(
                        "Overdue by " + minutesOverdue + "m. Did you think this was going to complete itself? Do it now!",
                        "It's been " + minutesOverdue + " minutes! Did the task file for bankruptcy?",
                        "I could have written a novel in the time you've spent ignoring this.",
                        "Congratulations, this task is now officially vintage.",
                        "Are we archiving this for future generations or are you going to click it?",
                        "Your streak is crying in the corner. Go comfort it by doing your job.",
                        "Do you need a dramatic musical score to help you start, or just basic willpower?",
                        "Stop reading my sarcasm and go click the finish button!",
                        "Even your computer fan sounds disappointed in this delay.",
                        "Enough procrastinating! Finish it right now or admit the task defeated you."
                );
            };
            case AGGRESSIVE -> switch (level) {
                case INITIAL -> List.of(
                        "Close the tabs and put down distractions. Start right now!",
                        "NO MORE WAITING! Start this task immediately!",
                        "LOCK IN! Time to execute without hesitation!",
                        "Quit stalling! Get your hands on the keys and move!",
                        "This is your wake-up call! Start working right now!",
                        "Stop overthinking and START DOING!",
                        "Zero distractions permitted. ATTACK THIS TASK!",
                        "Champions don't hesitate. START NOW!",
                        "Put your phone face down and get to work!",
                        "Focus! Your goals require effort, not daydreaming!"
                );
                case NUDGE -> List.of(
                        "You are " + minutesOverdue + " minutes late! What happened to your discipline?!",
                        "MOVE IT! " + minutesOverdue + " minutes wasted already!",
                        "Stop acting like you have all day! GET TO WORK!",
                        "Every second you waste is gone forever! FINISH THIS!",
                        "Why are you still reading notifications?! START THE TASK!",
                        "DO NOT SLEEP ON YOUR COMMITMENTS! MOVE!",
                        "You said you would do this! PROVE IT NOW!",
                        "Lateness is a choice! STOP CHOOSING EXCUSES!",
                        "Wake up! The clock is running and you are falling behind!",
                        "GET OFF SOCIAL MEDIA AND WORK!"
                );
                case WARN -> List.of(
                        "Stop making excuses. Finish this right this second or face the regret!",
                        "ARE YOU A QUITTER?! Finish this right this second!",
                        "NO EXCUSES ALLOWED! You are " + minutesOverdue + " minutes behind!",
                        "What happened to the person who scheduled this?! STEP UP!",
                        "You are stronger than this distraction! PUSH THROUGH!",
                        "Stop negotiating with weakness! GET IT DONE!",
                        "This is where habits are made or broken! DO NOT QUIT!",
                        "I don't care how tired you are! FINISH THE JOB!",
                        "Face the work! The discomfort lasts minutes, the pride lasts all day!",
                        "SHUT DOWN DISTRACTIONS AND CRUSH THIS TASK!"
                );
                case CRITICAL -> List.of(
                        "DROP EVERYTHING! You are " + minutesOverdue + " minutes late! Do it or admit defeat!",
                        "EMERGENCY LEVEL OVERDUE! Stop everything else and finish this right now!",
                        "THIS IS UNACCEPTABLE! " + minutesOverdue + " MINUTES OF NEGLECT! FIX IT NOW!",
                        "Are you really going to let a simple task defeat you?! GET UP AND DO IT!",
                        "DROP YOUR PHONE! CLOSE THE JUNK! FINISH THIS RIGHT NOW!",
                        "You are better than this lazy excuse! PROVE IT RIGHT THIS SECOND!",
                        "I REFUSE TO LET YOU QUIT ON YOURSELF! DO THE WORK!",
                        "ABSOLUTELY NO MORE POSTPONING! FINISH IT!",
                        "CRUSH THIS TASK RIGHT NOW AND RECLAIM YOUR SELF-RESPECT!",
                        "LAST CHANCE BEFORE TOTAL REGRET! DO IT NOW!"
                );
            };
        };

        int idx = ThreadLocalRandom.current().nextInt(pool.size());
        return pool.get(idx);
    }
}
