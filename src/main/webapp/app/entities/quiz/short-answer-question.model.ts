import { QuizQuestion, QuizQuestionType } from 'app/entities/quiz/quiz-question.model';
import { ShortAnswerMapping } from 'app/entities/quiz/short-answer-mapping.model';
import { ShortAnswerSolution } from 'app/entities/quiz/short-answer-solution.model';
import { ShortAnswerSpot } from 'app/entities/quiz/short-answer-spot.model';

export class ShortAnswerQuestion extends QuizQuestion {
    public spots?: ShortAnswerSpot[];
    public solutions?: ShortAnswerSolution[];
    public correctMappings?: ShortAnswerMapping[];
    public matchLetterCase = false;
    public similarityValue = 85;

    constructor() {
        super(QuizQuestionType.SHORT_ANSWER);
    }
}
