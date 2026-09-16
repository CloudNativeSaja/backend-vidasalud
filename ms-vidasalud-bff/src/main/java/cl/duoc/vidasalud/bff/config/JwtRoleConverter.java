package cl.duoc.vidasalud.bff.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;


@Component
public class JwtRoleConverter 
        implements Converter<Jwt, AbstractAuthenticationToken> {


    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {


        List<String> roles = 
                jwt.getClaimAsStringList("roles");


        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>();


        if (roles != null) {

            roles.forEach(role -> {

                authorities.add(
                    new SimpleGrantedAuthority(
                        "ROLE_" + role
                    )
                );

            });

        }


        return new JwtAuthenticationToken(
                jwt,
                authorities
        );
    }

}