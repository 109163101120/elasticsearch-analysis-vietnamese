/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.lucene.analysis.vi;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import vn.hus.nlp.tokenizer.TokenizerProvider;
import vn.hus.nlp.tokenizer.tokens.TaggedWord;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;


/**
 * Vietnamese Tokenizer.
 *
 * @author duydo
 */
public class VietnameseTokenizer extends Tokenizer {

    private Iterator<TaggedWord> taggedWords;

    private int offset = 0;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

    private vn.hus.nlp.tokenizer.Tokenizer tokenizer;
    private String inputText;


    public VietnameseTokenizer() {
        super();
        tokenizer = AccessController.doPrivileged(
                (PrivilegedAction<vn.hus.nlp.tokenizer.Tokenizer>) () ->
                        TokenizerProvider.getInstance().getTokenizer()
        );
    }

    private void tokenize(Reader input) throws IOException {
        this.inputText = IOUtils.toString(input);
        tokenizer.tokenize(new StringReader(this.inputText));
        taggedWords = tokenizer.getResult().iterator();
    }

    @Override
    public final boolean incrementToken() throws IOException {
        clearAttributes();
        while (taggedWords.hasNext()) {
            final TaggedWord word = taggedWords.next();
            if (accept(word)) {
                posIncrAtt.setPositionIncrement(1);
                final int length = word.getText().length();
                typeAtt.setType(String.format("<%s>", word.getRule().getName().toUpperCase()));
                termAtt.copyBuffer(word.getText().toCharArray(), 0, length);
                final int start = inputText.indexOf(word.getText(), offset);
                offsetAtt.setOffset(correctOffset(start), correctOffset(start + length));
                offset = offsetAtt.endOffset();
                return true;
            }
        }
        return false;
    }

    /**
     * Only accept the word characters.
     */
    private final boolean accept(TaggedWord word) {
        final String type = word.getRule().getName().toLowerCase();
        if ("punctuation".equals(type) || "special".equals(type)) {
            return false;
        }
        return true;
    }

    @Override
    public final void end() throws IOException {
        super.end();
        final int finalOffset = correctOffset(offset);
        offsetAtt.setOffset(finalOffset, finalOffset);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        offset = 0;
        tokenize(input);
    }
}
