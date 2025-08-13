package cn.isliu.core;


import java.util.Objects;

public class Reply {
    private AddSheet addSheet;
    private CopySheet copySheet;
    private DeleteSheet deleteSheet;

    public Reply() {
    }

    public Reply(AddSheet addSheet, CopySheet copySheet, DeleteSheet deleteSheet) {
        this.addSheet = addSheet;
        this.copySheet = copySheet;
        this.deleteSheet = deleteSheet;
    }

    public AddSheet getAddSheet() {
        return addSheet;
    }

    public void setAddSheet(AddSheet addSheet) {
        this.addSheet = addSheet;
    }

    public CopySheet getCopySheet() {
        return copySheet;
    }

    public void setCopySheet(CopySheet copySheet) {
        this.copySheet = copySheet;
    }

    public DeleteSheet getDeleteSheet() {
        return deleteSheet;
    }

    public void setDeleteSheet(DeleteSheet deleteSheet) {
        this.deleteSheet = deleteSheet;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Reply reply = (Reply) o;
        return Objects.equals(addSheet, reply.addSheet) && Objects.equals(copySheet, reply.copySheet) && Objects.equals(deleteSheet, reply.deleteSheet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addSheet, copySheet, deleteSheet);
    }

    @Override
    public String toString() {
        return "Reply{" +
                "addSheet=" + addSheet +
                ", copySheet=" + copySheet +
                ", deleteSheet=" + deleteSheet +
                '}';
    }
}
