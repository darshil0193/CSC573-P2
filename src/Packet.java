public class Packet {
    private StringBuilder data;
    private StringBuilder header;
    private Boolean isFirstPacket = false;
    private Integer sequenceNumber = -1;
    private Integer nextPacketNumber = 0;

    Packet(StringBuilder data2, Boolean isFirstPacket2, Integer sequenceNumber2, Integer nextPacketNumber2) {
        data = data2;
        isFirstPacket = isFirstPacket2;
        sequenceNumber = sequenceNumber2;
        nextPacketNumber = nextPacketNumber2;
    }

    StringBuilder getData() {
        return data;
    }

    public void setData(StringBuilder data) {
        this.data = data;
    }

    public StringBuilder getHeader() {
        return header;
    }

    public void setHeader(StringBuilder header) {
        this.header = header;
    }

    public Boolean getFirstPacket() {
        return isFirstPacket;
    }

    public void setFirstPacket(Boolean firstPacket) {
        isFirstPacket = firstPacket;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public Integer getNextPacketNumber() {
        return nextPacketNumber;
    }

    public void setNextPacketNumber(Integer nextPacketNumber) {
        this.nextPacketNumber = nextPacketNumber;
    }
}
