package it.petretiandrea.common;

import it.petretiandrea.utils.Utils;

import javax.rmi.CORBA.Util;
import java.io.UnsupportedEncodingException;

public class TopicMatcher {

    /**
     * È valido se non contiene i caratteri + o #, e la lunghezza non supera 65535 bytes
     * @param pubTopic Topic da pubblicare
     * @return True se valido, False se invalido
     */
    public static boolean isValidTopicPublish(String pubTopic) {

        try {
            if(pubTopic.getBytes(Utils.CHARSET).length <= 65535) {
                return !pubTopic.contains("+") && !pubTopic.contains("#");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Controlla se il topic contiene + e # nella posizione corretta. E se la lunghezza non supera 65535 bytes.
     * Example:
     *      “sport/tennis/#” is valid
     *      “sport/tennis#” is not valid CASE 1
     *      “sport/tennis/#/ranking” is not valid CASE 2
     *      “sport+” is not valid CASE 3
     * @param subTopic Topic di sottiscrizione da verificare
     * @return True se valido, False se invalido.
     */
    public static boolean isValidSubscribeTopic(String subTopic) {

        try {
            if(subTopic.getBytes(Utils.CHARSET).length <= 65535) {

                char[] topicArray = subTopic.toCharArray();
                boolean valid = true;
                for(int i = 0, prev = -1, next = 1; i < topicArray.length && valid; prev++, i++, next++) {
                    if(topicArray[i] == '+')
                        valid = !((prev > -1 && topicArray[prev] != '/') || (next < topicArray.length && topicArray[next] != '/')); // case 3
                    else if(topicArray[i] == '#')
                        valid = !((prev > -1 && topicArray[prev] != '/') || (next < topicArray.length)); // case 1 e 2
                }
                return valid;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean matchTopic(String filter, String topic) {

        int filterStart, topicStart;
        int filterEnd, topicEnd;

        filterStart = topicStart = 0;
        filterEnd = filter.length();
        topicEnd = topic.length();

        if(!filter.equals(topic)) {
            while(filterStart < filterEnd && topicStart < topicEnd) {
                // fix per casi in cui si ha ciao/#
                if(topicStart == filterEnd - 3 && filter.charAt(topicStart + 1) == '/' && filter.charAt(topicStart + 2) == '#') { // check for /#
                    topicStart = topicEnd;
                    filterStart = filterEnd;
                    break;
                }
                if(topic.charAt(topicStart) == '/' && filter.charAt(filterStart) != '/')
                    break;
                if (filter.charAt(filterStart) != '+' && filter.charAt(filterStart) != '#' &&
                        filter.charAt(filterStart) != topic.charAt(topicStart))
                    break;
                if(filter.charAt(filterStart) == '+') {
                    // scorre il topic fino a raggiungere la fine o un separatore
                    for(int next = topicStart + 1; next < topicEnd && topic.charAt(next) != '/'; ++topicStart, next = topicStart + 1);
                } else if(filter.charAt(filterStart) == '#')
                    topicStart = topicEnd - 1;
                filterStart++;
                topicStart++;
            }
            return (topicStart == topicEnd) && (filterStart == filterEnd);
        } else return true;
    }

    public static void main(String[] args) {

        System.out.println(matchTopic("sport/tennis/+", "sport/tennis/player1"));
        System.out.println(matchTopic("sport/tennis/+", "sport/tennis/player2"));
        System.out.println(matchTopic("sport/tennis/+", "sport/tennis/player1/ranking"));
        System.out.println(matchTopic("sport/+", "sport"));

        System.out.println(matchTopic("sport/+", "sport/55"));

        System.out.println(matchTopic("sport/tennis/player1/#/ciao", "sport/tennis/player1/ciao"));
        System.out.println(matchTopic("sport/tennis/player1/#", "sport/tennis/player1"));
        System.out.println(matchTopic("sport/tennis/player1/#", "sport/tennis/player1/score/wimbledo"));
    }

/*
    public static boolean isTopicMatchSubscription(String subscription, String topic) {
        int spos, tpos;
        boolean multilevel_wildcard = false;

        if(subscription.isEmpty() || topic.isEmpty()) return false;

        char[] subArray = subscription.toCharArray();
        char[] topicArray = topic.toCharArray();

        if((subArray[0] == '$' && topicArray[0] != '$')
                || (topicArray[0] == '$' && subArray[0] != '$')){

            return false;
        }

        spos = 0;
        tpos = 0;

        while(spos < slen && tpos < tlen){
            if(sub[spos] == topic[tpos]){
                if(tpos == tlen-1){

                    if(spos == slen-3
                            && sub[spos+1] == '/'
                            && sub[spos+2] == '#'){
					result = true;
                        multilevel_wildcard = true;
                        return MOSQ_ERR_SUCCESS;
                    }
                }
                spos++;
                tpos++;
                if(spos == slen && tpos == tlen){
				*result = true;
                    return MOSQ_ERR_SUCCESS;
                }else if(tpos == tlen && spos == slen-1 && sub[spos] == '+'){
                    spos++;
				*result = true;
                    return MOSQ_ERR_SUCCESS;
                }
            }else{
                if(sub[spos] == '+'){
                    spos++;
                    while(tpos < tlen && topic[tpos] != '/'){
                        tpos++;
                    }
                    if(tpos == tlen && spos == slen){
					*result = true;
                        return MOSQ_ERR_SUCCESS;
                    }
                }else if(sub[spos] == '#'){
                    multilevel_wildcard = true;
                    if(spos+1 != slen){
					*result = false;
                        return MOSQ_ERR_SUCCESS;
                    }else{
					*result = true;
                        return MOSQ_ERR_SUCCESS;
                    }
                }else{
				*result = false;
                    return MOSQ_ERR_SUCCESS;
                }
            }
        }
        if(multilevel_wildcard == false && (tpos < tlen || spos < slen)){
		*result = false;
        }

        return MOSQ_ERR_SUCCESS;
    }*/

}
