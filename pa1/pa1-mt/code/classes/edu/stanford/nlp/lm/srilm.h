#ifndef SRILMWRAP_H
#define SRILMWRAP_H

#ifdef __cplusplus
  extern "C" {
#endif

Vocab* initVocab(int start, int end);

Ngram* initLM(int order, Vocab* vocab);

int readLM(Ngram* ngram, const char* filename);

int readLM_limitVocab(Ngram* ngram, Vocab* vocab, const char* filename, const char* vocabFilename);

float getProb(Ngram* ngram, unsigned *context, int hist_size, unsigned cur_wrd);

float getWordProb(Ngram* ngram, unsigned w, unsigned* context);

float getSentenceProb(Ngram* ngram, unsigned* sentence);

unsigned getDepth(Ngram* ngram, unsigned *context, int hist_size);

unsigned getIndexForWord(Vocab* vo, const char *s);

int getVocab_None();

#ifdef __cplusplus
  }
#endif

#endif
