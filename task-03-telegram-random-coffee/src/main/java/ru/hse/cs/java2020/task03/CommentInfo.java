package ru.hse.cs.java2020.task03;

public class CommentInfo {
    public CommentInfo(String givenAuthor, String givenText) {
        this.author = givenAuthor;
        this.text = givenText;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String newAuthor) {
        this.author = newAuthor;
    }

    public String getText() {
        return text;
    }

    public void setText(String newText) {
        this.text = newText;
    }

    private String author;
    private String text;
}
