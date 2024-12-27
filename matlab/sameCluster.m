function same = sameCluster(clusters1, clusters2)
%compare whether two clusters are the same.
    same = 1;
    n1 = length(clusters1);
    n2 = length(clusters2);
    if (n1 ~= n2)
        same = 0;
        return;
    end
    for i=1:n1
        nodes1 = clusters1{i};
        nodes2 = clusters2{i};
        if (length(nodes1) ~= length(nodes2))
            same = 0;
            return;
        end
        for j=1:length(nodes1)
            if (nodes1(j) ~= nodes2(j))
                same = 0;
                return;
            end
        end
    end
end